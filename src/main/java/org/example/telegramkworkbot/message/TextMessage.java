package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public record TextMessage(
        Long chatId,
        String text
) implements OutgoingMessage {


    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }
}
