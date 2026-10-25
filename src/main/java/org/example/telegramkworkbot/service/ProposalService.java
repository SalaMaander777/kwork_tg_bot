package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.ai.LlmClient;
import org.example.telegramkworkbot.ai.LlmException;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.entities.Proposal;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.Html;
import org.example.telegramkworkbot.message.KeyboardMessage;
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

/** Генерация отклика на заказ через LLM. */
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
    private final MessageSender sender;

    @Value("${ai.daily-limit:20}")
    private int dailyLimit;

    /** Выполняется в отдельном потоке: ответ модели может идти десятки секунд. */
    @Async
    public void generate(Long chatId, Long userId, Long kworkId) {
        Optional<KworkEntity> kwork = kworkRepo.findById(kworkId);
        if (kwork.isEmpty()) {
            sender.send(new TextMessage(chatId, "Заказ не найден — возможно, он уже снят с публикации."));
            return;
        }
        if (usedToday(userId) >= dailyLimit) {
            sender.send(new TextMessage(chatId, "Лимит на сегодня исчерпан (" + dailyLimit + " откликов). Попробуйте завтра."));
            return;
        }

        sender.send(new TypingAction(chatId));
        String text;
        try {
            text = llm.complete(promptBuilder.build(kwork.get(), profileService.findAbout(userId)));
        } catch (LlmException e) {
            log.warn("Не удалось сгенерировать отклик для заказа {}: {}", kworkId, e.getMessage());
            sender.send(new TextMessage(chatId, "⚠️ AI временно недоступен, попробуйте позже."));
            return;
        }
        text = Html.truncate(text, MAX_PROPOSAL_LENGTH);
        save(userId, kworkId, text);

        String message = "✍️ <b>Отклик на «" + Html.escape(Html.truncate(kwork.get().getName(), 80)) + "»</b>\n\n"
                + Html.escape(text);
        sender.send(new KeyboardMessage(chatId, message, Keyboard.keyboard(
                Keyboard.button("🔄 Ещё вариант", "reply", "gen:" + kworkId),
                Keyboard.urlButton("🔗 Открыть на Kwork", KworkCardFormatter.url(kworkId)))));
    }

    private long usedToday(Long userId) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        return proposalRepo.countByUserIdAndCreatedAtAfter(userId, startOfDay);
    }

    private void save(Long userId, Long kworkId, String text) {
        Proposal proposal = new Proposal();
        proposal.setUserId(userId);
        proposal.setKworkId(kworkId);
        proposal.setText(text);
        proposal.setCreatedAt(Instant.now());
        proposalRepo.save(proposal);
    }
}
