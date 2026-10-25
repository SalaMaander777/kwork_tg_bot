package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.ai.LlmClient;
import org.example.telegramkworkbot.ai.LlmException;
import org.example.telegramkworkbot.ai.LlmOptions;
import org.example.telegramkworkbot.ai.LlmResult;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.entities.Proposal;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.message.EditMarkup;
import org.example.telegramkworkbot.message.EditMessage;
import org.example.telegramkworkbot.message.Html;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.message.TypingAction;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.repository.ProposalRepo;
import org.example.telegramkworkbot.repository.UserRepo;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.settings.SettingsService;
import org.example.telegramkworkbot.tracking.EventRecorder;
import org.example.telegramkworkbot.tracking.EventType;
import org.example.telegramkworkbot.tracking.LlmCallRecorder;
import org.example.telegramkworkbot.tracking.LlmCallRecorder.LlmCall;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Генерация откликов через LLM. Карточка заказа и отклики живут в одном сообщении:
 * оно перерисовывается между видами «заказ» и «отклик».
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    // лимит Telegram — 4096 символов на сообщение, оставляем место под заголовок
    private static final int MAX_PROPOSAL_LENGTH = 3800;

    private final KworkRepo kworkRepo;
    private final ProposalRepo proposalRepo;
    private final UserRepo userRepo;
    private final ProfileService profileService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llm;
    private final KworkCardFormatter cardFormatter;
    private final ProposalView proposalView;
    private final MessageSender sender;
    private final SettingsService settings;
    private final LlmCallRecorder callRecorder;
    private final EventRecorder events;

    /** Персональный лимит пользователя или общий из настроек. */
    public int dailyLimit(Long userId) {
        Integer override = userRepo.findById(userId).map(UserEntity::getDailyLimitOverride).orElse(null);
        return override != null ? override : settings.dailyLimit();
    }

    public boolean limitReached(Long userId) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        boolean reached = proposalRepo.countByUserIdAndCreatedAtAfter(userId, startOfDay) >= dailyLimit(userId);
        if (reached) {
            events.record(userId, EventType.LIMIT_HIT);
        }
        return reached;
    }

    /** Выполняется в отдельном потоке: ответ модели может идти десятки секунд. */
    @Async
    public void generate(Long chatId, Integer messageId, Long userId, Long kworkId) {
        Optional<KworkEntity> kwork = kworkRepo.findById(kworkId);
        if (kwork.isEmpty()) {
            sender.send(new TextMessage(chatId, "Заказ не найден — возможно, он уже снят с публикации."));
            return;
        }

        sender.send(new EditMarkup(chatId, messageId, proposalView.loading()));
        sender.send(new TypingAction(chatId));

        LlmResult result;
        Long callId;
        try {
            Timed timed = complete(userId, kwork.get(), profileService.findAbout(userId), settings.systemPrompt());
            result = timed.result();
            callId = timed.callId();
        } catch (LlmException e) {
            log.warn("Не удалось сгенерировать отклик для заказа {}: {}", kworkId, e.getMessage());
            showCard(chatId, messageId, userId, kwork.get());
            sender.send(new TextMessage(chatId, "⚠️ AI временно недоступен, попробуйте позже."));
            return;
        }

        Proposal proposal = save(userId, kworkId, Html.truncate(result.text(), MAX_PROPOSAL_LENGTH), callId);
        events.record(userId, EventType.PROPOSAL_GENERATED, String.valueOf(kworkId));
        render(chatId, messageId, kwork.get(), proposal);
    }

    public record Timed(LlmResult result, Long callId, long durationMs) {}

    /**
     * Вызов модели с записью в журнал llm_calls. Используется и ботом, и тестом промпта в админке.
     *
     * @throws LlmException если модель не ответила (вызов тоже записывается)
     */
    public Timed complete(Long userId, KworkEntity kwork, Optional<String> about, String systemPrompt) {
        String model = settings.aiModel();
        Instant startedAt = Instant.now();
        long start = System.nanoTime();
        try {
            LlmResult result = llm.complete(promptBuilder.build(kwork, about, systemPrompt),
                    new LlmOptions(model, settings.aiTemperature()));
            long ms = (System.nanoTime() - start) / 1_000_000;
            Long callId = callRecorder.record(new LlmCall(userId, kwork.getId(), result.model(), startedAt, ms,
                    true, result.promptTokens(), result.completionTokens(), null));
            return new Timed(result, callId, ms);
        } catch (LlmException e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            callRecorder.record(new LlmCall(userId, kwork.getId(), model, startedAt, ms, false, null, null, e.getMessage()));
            throw e;
        }
    }

    /** Показать конкретный вариант отклика (кнопки «Мой отклик», ◀️ ▶️). */
    public void show(Long chatId, Integer messageId, Long userId, Long proposalId) {
        proposalRepo.findById(proposalId)
                .filter(proposal -> proposal.getUserId().equals(userId))
                .ifPresent(proposal -> kworkRepo.findById(proposal.getKworkId())
                        .ifPresent(kwork -> {
                            events.record(userId, EventType.PROPOSAL_VIEWED, String.valueOf(proposalId));
                            render(chatId, messageId, kwork, proposal);
                        }));
    }

    /** Вернуть сообщению вид карточки заказа. */
    public void showCard(Long chatId, Integer messageId, Long userId, Long kworkId) {
        kworkRepo.findById(kworkId).ifPresent(kwork -> showCard(chatId, messageId, userId, kwork));
    }

    private void showCard(Long chatId, Integer messageId, Long userId, KworkEntity kwork) {
        Optional<Long> latest = proposalRepo.findFirstByUserIdAndKworkIdOrderByIdDesc(userId, kwork.getId())
                .map(Proposal::getId);
        sender.send(new EditMessage(chatId, messageId, cardFormatter.text(kwork), cardFormatter.keyboard(kwork, latest)));
    }

    private void render(Long chatId, Integer messageId, KworkEntity kwork, Proposal proposal) {
        var variants = proposalRepo.findByUserIdAndKworkIdOrderByIdAsc(proposal.getUserId(), proposal.getKworkId());
        sender.send(new EditMessage(chatId, messageId,
                proposalView.text(kwork, proposal), proposalView.keyboard(proposal, variants)));
    }

    private Proposal save(Long userId, Long kworkId, String text, Long callId) {
        Proposal proposal = new Proposal();
        proposal.setUserId(userId);
        proposal.setKworkId(kworkId);
        proposal.setText(text);
        proposal.setCreatedAt(Instant.now());
        proposal.setLlmCallId(callId);
        return proposalRepo.save(proposal);
    }
}
