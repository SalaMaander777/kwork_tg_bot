package org.example.telegramkworkbot.ai;

/** Параметры конкретного вызова; null — значение по умолчанию провайдера. */
public record LlmOptions(String model, Double temperature) {

    public static final LlmOptions DEFAULT = new LlmOptions(null, null);
}
