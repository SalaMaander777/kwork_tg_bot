package org.example.telegramkworkbot.commands;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;


@Component
@RequiredArgsConstructor
public class HelpCommand implements CommandHandler{

    private final MessageSender messageSender;


    @Override
    public String command() {
        return "/help";
    }

    @Override
    public void handle(Message message) {
        messageSender.send(new TextMessage(message.getChatId(), "Я бот который помогает автоматизировать поиск вакансий на kwork"));
    }
}
