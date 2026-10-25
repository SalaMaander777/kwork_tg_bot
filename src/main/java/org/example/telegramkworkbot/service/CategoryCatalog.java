package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.JsonCategoryMapper;
import org.example.telegramkworkbot.dto.KworkCategoryDto;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Дерево категорий Kwork из kwork_categories.json, загружается один раз при старте.
 * kwork.category_id в данных парсера — это id подкатегории.
 */
@Component
public class CategoryCatalog {

    private final List<KworkCategoryDto> roots;
    private final Map<Long, KworkCategoryDto> rootsById = new HashMap<>();
    private final Map<Long, KworkSubcategoryDto> subsById = new HashMap<>();

    public CategoryCatalog(JsonCategoryMapper mapper) {
        this.roots = List.copyOf(mapper.map());
        for (KworkCategoryDto root : roots) {
            rootsById.put(root.getId(), root);
            for (KworkSubcategoryDto sub : root.getCats()) {
                subsById.put(sub.getId(), sub);
            }
        }
    }

    public List<KworkCategoryDto> roots() {
        return roots;
    }

    public Optional<KworkCategoryDto> root(long id) {
        return Optional.ofNullable(rootsById.get(id));
    }

    public Optional<KworkSubcategoryDto> sub(long id) {
        return Optional.ofNullable(subsById.get(id));
    }

    public List<Long> subIds(KworkCategoryDto root) {
        return root.getCats().stream().map(KworkSubcategoryDto::getId).toList();
    }
}
