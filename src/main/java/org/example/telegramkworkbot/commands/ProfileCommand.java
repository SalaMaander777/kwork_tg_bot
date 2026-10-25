package org.example.telegramkworkbot.commands;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.service.ProfileService;
import org.example.telegramkworkbot.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class ProfileCommand implements CommandHandler {

    private final UserService userService;
    private final ProfileService profileService;

    @Override
    public String command() {
        return "/profile";
    }

    @Override
    public void handle(Message message) {
        userService.registerOrUpdateUser(message.getFrom());
        profileService.showProfile(message.getChatId(), message.getFrom().getId());
    }
}
