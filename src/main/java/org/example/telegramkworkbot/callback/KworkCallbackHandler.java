package org.example.telegramkworkbot.callback;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.message.RemoveMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/** Кнопки под карточкой заказа: kwork:hide — убрать карточку из чата. */
@Component
@RequiredArgsConstructor
public class KworkCallbackHandler implements CallbackHandler {

    private final MessageSender sender;

    @Override
    public String action() {
        return "kwork";
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long chatId = query.getMessage().getChatId();
        if ("hide".equals(data.payload())) {
            sender.send(new RemoveMessage(chatId, query.getMessage().getMessageId()));
        }
        sender.send(new CallbackAnswer(chatId, query.getId(), null));
    }
}
