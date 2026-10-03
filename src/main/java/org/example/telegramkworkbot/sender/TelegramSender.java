package org.example.telegramkworkbot.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.message.OutgoingMessage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;


@Slf4j
@RequiredArgsConstructor
@Component
public class TelegramSender implements MessageSender{
    private final TelegramClient client;

    @Override
    public void send(OutgoingMessage message) {
        try {
            message.deliver(client);
        } catch (TelegramApiException e) {
           log.error("Ошибка сообщения в чат: {}", message.chatId(), e);
        }
    }
}
