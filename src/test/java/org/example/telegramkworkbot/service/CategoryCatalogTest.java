package org.example.telegramkworkbot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCatalogTest {

    private final CategoryCatalog catalog = TestCatalog.load();

    @Test
    void loadsTreeFromJson() {
        assertThat(catalog.roots()).isNotEmpty();
        assertThat(catalog.root(17)).hasValueSatisfying(root -> assertThat(root.getName()).isEqualTo("SEO и трафик"));
        assertThat(catalog.sub(44)).hasValueSatisfying(sub -> assertThat(sub.getName()).isEqualTo("SEO аудиты, консультации"));
        assertThat(catalog.sub(17)).isEmpty();
    }

    @Test
    void callbackDataFitsTelegramLimit() {
        // callback_data в Telegram — не больше 64 байт
        catalog.roots().forEach(root -> catalog.subIds(root).forEach(id ->
                assertThat(("category:toggle:" + id).getBytes()).hasSizeLessThanOrEqualTo(64)));
    }
}
