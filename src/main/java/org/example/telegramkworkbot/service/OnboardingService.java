package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;
    private final CategorySelectionService selectionService;
    private final MessageSender sender;


    public void onStart(User tgUser, Long chatId) {
        boolean isNew = userService.registerOrUpdateUser(tgUser);

        if (!isNew) {
            sender.send(new TextMessage(chatId, "С возвращением! 👋"));
            return;
        }

        userService.setState(tgUser.getId(), UserState.CHOOSING_CATEGORY);
        selectionService.start(chatId);
    }
}
