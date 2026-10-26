package org.example.telegramkworkbot.ai;

/** Ответ модели и расход токенов (null, если провайдер их не вернул). */
public record LlmResult(String text, String model, Integer promptTokens, Integer completionTokens) {
}
