package org.example.telegramkworkbot.commands;

import lombok.RequiredArgsConstructor;

import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.TextMessage;

import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.service.CategorySelectionService;
import org.example.telegramkworkbot.service.StateService;
import org.example.telegramkworkbot.service.UserService;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.message.Message;


@Component
@RequiredArgsConstructor
public class StartCommand implements CommandHandler{
    private final MessageSender sender;
    private final UserService userService;
    private final StateService stateService;
    private final CategorySelectionService selectionService;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(Message message) {
        boolean isNew = userService.registerOrUpdateUser(message.getFrom());
        if (isNew){
            stateService.setState(
                    message.getFrom().getId(),
                    UserState.CHOOSING_CATEGORY
            );
            selectionService.start(message);
            return;
        }

        sender.send(
                new TextMessage(
                        message.getChatId(),
                        "С возвращением! 👋"
                )
        );
    }
}
