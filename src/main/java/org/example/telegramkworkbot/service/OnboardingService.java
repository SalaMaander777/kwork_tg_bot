package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.stereotype.Service;
import org.example.telegramkworkbot.tracking.EventRecorder;
import org.example.telegramkworkbot.tracking.EventType;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;
    private final CategorySelectionService selectionService;
    private final ProfileService profileService;
    private final MessageSender sender;
    private final EventRecorder events;


    public void onStart(User tgUser, Long chatId) {
        UserEntity user = userService.registerOrUpdateUser(tgUser);
        events.record(tgUser.getId(), EventType.START, user.getState().name());
        switch (user.getState()) {
            case ACTIVE -> sender.send(new TextMessage(chatId,
                    "С возвращением! 👋 Новые заказы будут приходить сюда.\n/categories — категории, /profile — профиль"));
            case BLOCKED -> {
                // пользователь разблокировал бота и снова нажал /start
                userService.setState(tgUser.getId(), UserState.ACTIVE);
                sender.send(new TextMessage(chatId, "С возвращением! 👋 Рассылка заказов снова включена."));
            }
            case FILLING_PROFILE -> profileService.askProfile(chatId, tgUser.getId());
            case NEW, CHOOSING_CATEGORY -> {
                userService.setState(tgUser.getId(), UserState.CHOOSING_CATEGORY);
                selectionService.start(chatId, tgUser.getId());
            }
        }
    }
}
