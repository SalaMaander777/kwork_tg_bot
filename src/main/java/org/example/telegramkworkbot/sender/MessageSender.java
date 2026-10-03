package org.example.telegramkworkbot.sender;

import org.example.telegramkworkbot.message.OutgoingMessage;

public interface MessageSender {
    void send(OutgoingMessage message);
}
