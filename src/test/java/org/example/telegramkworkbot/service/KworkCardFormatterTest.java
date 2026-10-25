package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class KworkCardFormatterTest {

    private final KworkCardFormatter formatter = new KworkCardFormatter(TestCatalog.load());

    @Test
    void formatsCardAndEscapesHtml() {
        KworkEntity kwork = kwork("Парсер <сайта> & бот", "a".repeat(1000));

        String text = formatter.text(kwork);

        assertThat(text).contains("<b>Парсер &lt;сайта&gt; &amp; бот</b>")
                .contains("💰 до 15 000 ₽ · ⏱ 5 дн. · 💬 откликов: 7")
                .contains("📂 SEO аудиты, консультации")
                .contains("👤 buyer")
                .contains("…")
                .doesNotContain("a".repeat(KworkCardFormatter.MAX_DESCRIPTION_LENGTH + 1));
    }

    @Test
    void keyboardHasReplyAndLink() {
        var buttons = formatter.keyboard(kwork("x", "y")).getKeyboard().get(0);
        assertThat(buttons.get(0).getCallbackData()).isEqualTo("reply:gen:42");
        assertThat(buttons.get(1).getUrl()).isEqualTo("https://kwork.ru/projects/42");
    }

    static KworkEntity kwork(String name, String description) {
        KworkEntity kwork = new KworkEntity();
        kwork.setId(42L);
        kwork.setName(name);
        kwork.setDescription(description);
        kwork.setPriceLimit(new BigDecimal("15000.00"));
        kwork.setMaxDays(5);
        kwork.setCategoryId(44L);
        kwork.setUsername("buyer");
        kwork.setKworkCount(7);
        return kwork;
    }
}
