package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.ai.ChatMessage;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.message.Html;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Промпт для генерации отклика на заказ. */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    static final int MAX_DESCRIPTION_LENGTH = 3000;

    /** Системный промпт по умолчанию; в работе используется значение из настроек. */
    public static final String SYSTEM = """
            Ты — опытный фрилансер на бирже Kwork. Пиши отклик на заказ покупателя от первого лица.

            Требования:
            - 600–900 символов, на русском, вежливо и по делу;
            - без шаблонных фраз вроде «Здравствуйте, я профессионал с большим опытом»;
            - покажи, что понял задачу: кратко опиши, как будешь её решать (2–4 шага);
            - если в описании чего-то не хватает — задай 1–2 уточняющих вопроса;
            - опирайся только на опыт из описания исполнителя, ничего не выдумывай;
            - не называй цену и сроки, если их нет в заказе;
            - без markdown-разметки, приветствия-подписи и эмодзи — только текст отклика.""";

    private final CategoryCatalog catalog;

    public List<ChatMessage> build(KworkEntity kwork, Optional<String> about, String systemPrompt) {
        StringBuilder sb = new StringBuilder("Заказ: ").append(kwork.getName()).append('\n');
        if (kwork.getCategoryId() != null) {
            catalog.sub(kwork.getCategoryId())
                    .map(KworkSubcategoryDto::getName)
                    .ifPresent(name -> sb.append("Категория: ").append(name).append('\n'));
        }
        if (kwork.getPriceLimit() != null) {
            sb.append("Бюджет: до ").append(kwork.getPriceLimit().toPlainString()).append(" ₽\n");
        }
        if (kwork.getMaxDays() != null) {
            sb.append("Срок: ").append(kwork.getMaxDays()).append(" дн.\n");
        }
        sb.append("Описание:\n")
                .append(Html.truncate(kwork.getDescription() == null ? "" : kwork.getDescription().strip(), MAX_DESCRIPTION_LENGTH))
                .append("\n\n");

        sb.append("Об исполнителе:\n").append(about
                .filter(text -> !text.isBlank())
                .orElse("Описание не заполнено — не указывай конкретный опыт и проекты, пиши в общем виде."));

        return List.of(ChatMessage.system(systemPrompt), ChatMessage.user(sb.toString()));
    }
}
