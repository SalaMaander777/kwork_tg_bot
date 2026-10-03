package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public interface OutgoingMessage {
    Long chatId();
    void deliver(TelegramClient client) throws TelegramApiException;

}
