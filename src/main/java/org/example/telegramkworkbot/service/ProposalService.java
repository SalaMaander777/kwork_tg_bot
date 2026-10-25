package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.ai.LlmClient;
import org.example.telegramkworkbot.ai.LlmException;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.entities.Proposal;
import org.example.telegramkworkbot.message.EditMarkup;
import org.example.telegramkworkbot.message.EditMessage;
import org.example.telegramkworkbot.message.Html;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.message.TypingAction;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.repository.ProposalRepo;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.beans.factory.annotation.Value;
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
    private final ProfileService profileService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llm;
    private final KworkCardFormatter cardFormatter;
    private final ProposalView proposalView;
    private final MessageSender sender;

    @Value("${ai.daily-limit:20}")
    private int dailyLimit;

    public boolean limitReached(Long userId) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        return proposalRepo.countByUserIdAndCreatedAtAfter(userId, startOfDay) >= dailyLimit;
    }

    public int dailyLimit() {
        return dailyLimit;
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
        String text;
        try {
            text = llm.complete(promptBuilder.build(kwork.get(), profileService.findAbout(userId)));
        } catch (LlmException e) {
            log.warn("Не удалось сгенерировать отклик для заказа {}: {}", kworkId, e.getMessage());
            showCard(chatId, messageId, userId, kwork.get());
            sender.send(new TextMessage(chatId, "⚠️ AI временно недоступен, попробуйте позже."));
            return;
        }

        Proposal proposal = save(userId, kworkId, Html.truncate(text, MAX_PROPOSAL_LENGTH));
        render(chatId, messageId, kwork.get(), proposal);
    }

    /** Показать конкретный вариант отклика (кнопки «Мой отклик», ◀️ ▶️). */
    public void show(Long chatId, Integer messageId, Long userId, Long proposalId) {
        proposalRepo.findById(proposalId)
                .filter(proposal -> proposal.getUserId().equals(userId))
                .ifPresent(proposal -> kworkRepo.findById(proposal.getKworkId())
                        .ifPresent(kwork -> render(chatId, messageId, kwork, proposal)));
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

    private Proposal save(Long userId, Long kworkId, String text) {
        Proposal proposal = new Proposal();
        proposal.setUserId(userId);
        proposal.setKworkId(kworkId);
        proposal.setText(text);
        proposal.setCreatedAt(Instant.now());
        return proposalRepo.save(proposal);
    }
}
