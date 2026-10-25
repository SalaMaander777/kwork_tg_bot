package org.example.telegramkworkbot.text;

import org.example.telegramkworkbot.enums.UserState;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/** Обработчик обычного текста (не команды) для пользователя в определённом состоянии. */
public interface TextStateHandler {
    UserState state();
    void handle(Message message);
}
