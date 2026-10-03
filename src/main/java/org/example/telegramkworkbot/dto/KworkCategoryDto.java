package org.example.telegramkworkbot.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class KworkCategoryDto {
    private long id;
    private String name;
    private List<KworkSubcategoryDto> cats;
}
