package org.example.telegramkworkbot.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.callback.CallbackData;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@RequiredArgsConstructor
@Service
public class CategorySelectionService {


    public void start(Message message){

    }
    public void handle(CallbackQuery query, CallbackData data){

    }
}
