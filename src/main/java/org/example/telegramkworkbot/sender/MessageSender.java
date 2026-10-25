package org.example.telegramkworkbot.sender;

import org.example.telegramkworkbot.message.OutgoingMessage;

public interface MessageSender {
    SendStatus send(OutgoingMessage message);
}
