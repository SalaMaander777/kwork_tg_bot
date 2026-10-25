package org.example.telegramkworkbot.keyboard;


import org.example.telegramkworkbot.callback.CallbackData;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public final class Keyboard {
    private Keyboard() {}


    public static InlineKeyboardButton button(String text, String action, String payload){
        return  InlineKeyboardButton.builder()
                .text(text)
                .callbackData(new CallbackData(action, payload).encode())
                .build();

    }

    public static InlineKeyboardButton urlButton(String text, String url) {
        return InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build();
    }


    public static InlineKeyboardMarkup keyboard(InlineKeyboardButton...buttons){
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(buttons))
                .build();
    }

    public static InlineKeyboardMarkup keyboard(List<InlineKeyboardRow> rows) {
        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    /** Раскладывает кнопки по строкам, perRow штук в строке. */
    public static List<InlineKeyboardRow> grid(List<InlineKeyboardButton> buttons, int perRow) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += perRow) {
            rows.add(new InlineKeyboardRow(buttons.subList(i, Math.min(i + perRow, buttons.size()))));
        }
        return rows;
    }

}
