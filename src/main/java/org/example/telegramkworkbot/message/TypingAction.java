package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Статус «печатает…», пока генерируется отклик. */
public record TypingAction(Long chatId) implements OutgoingMessage {

    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(SendChatAction.builder().chatId(chatId).action(ActionType.TYPING.toString()).build());
    }
}
