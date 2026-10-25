package org.example.telegramkworkbot.sender;

public enum SendStatus {
    OK,
    BLOCKED, // 403: пользователь заблокировал бота
    FAILED
}
