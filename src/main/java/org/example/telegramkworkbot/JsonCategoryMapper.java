package org.example.telegramkworkbot;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.example.telegramkworkbot.dto.KworkCategoryDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JsonCategoryMapper {

    private final ObjectMapper objectMapper;


    public List<KworkCategoryDto> map(){
        try(InputStream inputStream = getClass().getResourceAsStream("kwork_categories.json");){

            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }










}
