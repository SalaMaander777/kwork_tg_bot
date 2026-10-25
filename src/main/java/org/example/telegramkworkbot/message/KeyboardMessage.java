package org.example.telegramkworkbot.message;

import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Сообщение в HTML-разметке с inline-клавиатурой. Пользовательский текст экранировать через {@link Html#escape}. */
public record KeyboardMessage(
        Long chatId,
        String text,
        InlineKeyboardMarkup markup
) implements OutgoingMessage {

    @Override
    public void deliver(TelegramClient client) throws TelegramApiException {
        client.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(markup)
                .build());
    }
}
