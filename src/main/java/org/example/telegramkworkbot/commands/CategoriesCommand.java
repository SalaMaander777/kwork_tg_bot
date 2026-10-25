package org.example.telegramkworkbot.commands;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.service.CategorySelectionService;
import org.example.telegramkworkbot.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CategoriesCommand implements CommandHandler {

    private final UserService userService;
    private final CategorySelectionService selectionService;

    @Override
    public String command() {
        return "/categories";
    }

    @Override
    public void handle(Message message) {
        userService.registerOrUpdateUser(message.getFrom());
        selectionService.start(message.getChatId(), message.getFrom().getId());
    }
}
