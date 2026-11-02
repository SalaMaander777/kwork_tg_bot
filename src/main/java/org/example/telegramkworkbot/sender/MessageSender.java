package org.example.telegramkworkbot.sender;

import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.message.OutgoingMessage;

public interface MessageSender {
    SendStatus send(OutgoingMessage message);

    /** Результат отправки с id сообщения в чате (null, если id получить не удалось). */
    record Sent(SendStatus status, Integer messageId) {}

    /** Отправка с запоминанием id сообщения — чтобы потом его редактировать. */
    default Sent sendTracked(KeyboardMessage message) {
        return new Sent(send(message), null);
    }
}
