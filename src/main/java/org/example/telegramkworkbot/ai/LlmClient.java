package org.example.telegramkworkbot.ai;

import java.util.List;

/** Провайдер языковой модели. Реализации: GigaChat; позже можно добавить YandexGPT, Ollama и т.п. */
public interface LlmClient {

    /** @throws LlmException если модель недоступна или вернула пустой ответ */
    LlmResult complete(List<ChatMessage> messages, LlmOptions options);

    /** Модель, которая используется, если в options она не задана. */
    String defaultModel();
}
