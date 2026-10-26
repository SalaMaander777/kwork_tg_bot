package org.example.telegramkworkbot.callback;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.service.ProposalService;
import org.example.telegramkworkbot.settings.SettingsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * reply:gen:&lt;kworkId&gt; — сгенерировать новый вариант отклика;
 * reply:show:&lt;proposalId&gt; — показать вариант; reply:noop — неактивная кнопка.
 */
@Component
@RequiredArgsConstructor
public class ProposalCallbackHandler implements CallbackHandler {

    private final ProposalService proposalService;
    private final MessageSender sender;
    private final SettingsService settings;

    @Override
    public String action() {
        return "reply";
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();
        Long userId = query.getFrom().getId();
        CallbackData op = CallbackData.parse(data.payload());

        switch (op.action()) {
            case "gen" -> {
                if (!settings.aiEnabled()) {
                    sender.send(new CallbackAnswer(chatId, query.getId(), "AI временно отключён, попробуйте позже"));
                    return;
                }
                if (proposalService.limitReached(userId)) {
                    sender.send(new CallbackAnswer(chatId, query.getId(),
                            "Лимит на сегодня исчерпан (" + proposalService.dailyLimit(userId) + " откликов)"));
                    return;
                }
                sender.send(new CallbackAnswer(chatId, query.getId(), "Генерирую отклик…"));
                proposalService.generate(chatId, messageId, userId, Long.parseLong(op.payload()));
                return;
            }
            case "show" -> proposalService.show(chatId, messageId, userId, Long.parseLong(op.payload()));
            default -> { } // noop
        }
        sender.send(new CallbackAnswer(chatId, query.getId(), null));
    }
}
