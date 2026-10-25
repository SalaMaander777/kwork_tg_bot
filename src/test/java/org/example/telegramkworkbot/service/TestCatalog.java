package org.example.telegramkworkbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.telegramkworkbot.JsonCategoryMapper;

final class TestCatalog {
    private TestCatalog() {}

    /** Каталог из настоящего kwork_categories.json. */
    static CategoryCatalog load() {
        return new CategoryCatalog(new JsonCategoryMapper(new ObjectMapper()));
    }
}
