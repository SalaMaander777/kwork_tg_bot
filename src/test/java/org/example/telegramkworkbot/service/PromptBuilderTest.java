package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.ai.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder(TestCatalog.load());

    @Test
    void includesKworkAndProfile() {
        List<ChatMessage> messages = builder.build(
                KworkCardFormatterTest.kwork("SEO-аудит интернет-магазина", "Нужен аудит"),
                Optional.of("5 лет в SEO, Screaming Frog"));

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(messages.get(1).content())
                .contains("Заказ: SEO-аудит интернет-магазина")
                .contains("Категория: SEO аудиты, консультации")
                .contains("Бюджет: до 15000.00 ₽")
                .contains("Нужен аудит")
                .contains("5 лет в SEO, Screaming Frog");
    }

    @Test
    void truncatesLongDescriptionAndHandlesEmptyProfile() {
        String content = builder.build(
                KworkCardFormatterTest.kwork("x", "b".repeat(10_000)), Optional.empty()).get(1).content();

        assertThat(content).doesNotContain("b".repeat(PromptBuilder.MAX_DESCRIPTION_LENGTH + 1))
                .contains("Описание не заполнено");
    }
}
