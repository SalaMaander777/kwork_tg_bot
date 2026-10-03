package org.example.telegramkworkbot.commands;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.service.OnboardingService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class StartCommand implements CommandHandler {

    private final OnboardingService onboardingService;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(Message message) {
        onboardingService.onStart(message.getFrom(), message.getChatId());
    }
}