package org.example.telegramkworkbot.ai;

import java.util.List;

/** Провайдер языковой модели. Реализации: GigaChat; позже можно добавить YandexGPT, Ollama и т.п. */
public interface LlmClient {

    /** @throws LlmException если модель недоступна или вернула пустой ответ */
    String complete(List<ChatMessage> messages);
}
