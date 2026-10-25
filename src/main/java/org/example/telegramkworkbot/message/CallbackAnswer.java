package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Ответ на нажатие кнопки: убирает «часики», text (может быть null) показывается всплывашкой. */
public record CallbackAnswer(
        Long chatId,
        String queryId,
        String text
) implements OutgoingMessage {

    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(AnswerCallbackQuery.builder()
                .callbackQueryId(queryId)
                .text(text)
                .build());
    }
}
