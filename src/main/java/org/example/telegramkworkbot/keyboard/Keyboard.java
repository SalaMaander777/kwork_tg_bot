package org.example.telegramkworkbot.keyboard;


import org.example.telegramkworkbot.callback.CallbackData;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public final class Keyboard {
    private Keyboard() {}


    public static InlineKeyboardButton button(String text, String action, String payload){
        return  InlineKeyboardButton.builder()
                .text(text)
                .callbackData(new CallbackData(action, payload).encode())
                .build();

    }


    public static InlineKeyboardMarkup keyboard(InlineKeyboardButton...buttons){
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(buttons))
                .build();
    }

}
