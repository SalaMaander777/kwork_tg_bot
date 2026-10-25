package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public record RemoveMessage(
        Long chatId,
        Integer messageId
) implements OutgoingMessage {

    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
    }
}
