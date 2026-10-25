package org.example.telegramkworkbot.callback;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.message.RemoveMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.service.ProposalService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * Кнопки сообщения с заказом: kwork:hide — убрать из чата,
 * kwork:card:&lt;kworkId&gt; — вернуться от отклика к карточке.
 */
@Component
@RequiredArgsConstructor
public class KworkCallbackHandler implements CallbackHandler {

    private final ProposalService proposalService;
    private final MessageSender sender;

    @Override
    public String action() {
        return "kwork";
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();
        CallbackData op = CallbackData.parse(data.payload());

        switch (op.action()) {
            case "hide" -> sender.send(new RemoveMessage(chatId, messageId));
            case "card" -> proposalService.showCard(chatId, messageId, query.getFrom().getId(), Long.parseLong(op.payload()));
            default -> { }
        }
        sender.send(new CallbackAnswer(chatId, query.getId(), null));
    }
}
