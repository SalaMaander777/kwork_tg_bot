package org.example.telegramkworkbot.service;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.callback.CallbackData;
import org.example.telegramkworkbot.callback.CallbackHandler;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * Выбор категорий. Сам является CallbackHandler для действия "category",
 * поэтому отдельная обёртка CategoryCallbackHandler больше не нужна.
 */
@Slf4j
@Service
public class CategorySelectionService implements CallbackHandler {

    @Override
    public String action() {
        return "category";
    }

    /** Вызывается из онбординга: показать первое меню выбора категорий. */
    public void start(Long chatId) {
        // TODO: отправить клавиатуру с категориями
    }

    /** Нажатие на кнопку категории/подкатегории. */
    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        // TODO: сохранить выбор, показать следующий уровень
    }
}