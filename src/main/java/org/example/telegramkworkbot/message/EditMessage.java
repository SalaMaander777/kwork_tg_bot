package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Перерисовка уже отправленного сообщения (меню категорий и т.п.). */
public record EditMessage(
        Long chatId,
        Integer messageId,
        String text,
        InlineKeyboardMarkup markup
) implements OutgoingMessage {

    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .replyMarkup(markup)
                .build());
    }
}
