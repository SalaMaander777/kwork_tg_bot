package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.callback.CallbackData;
import org.example.telegramkworkbot.callback.CallbackHandler;
import org.example.telegramkworkbot.entities.FreelancerProfile;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.message.EditMessage;
import org.example.telegramkworkbot.message.Html;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.repository.ProfileRepo;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.text.TextStateHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.util.Optional;

/**
 * Профиль фрилансера — свободный текст о себе, на его основе AI пишет отклики.
 * Принимает текст в состоянии FILLING_PROFILE и кнопки действия "profile" (skip | edit).
 */
@Service
@RequiredArgsConstructor
public class ProfileService implements CallbackHandler, TextStateHandler {

    private static final String ACTION = "profile";
    private static final int MAX_ABOUT_LENGTH = 2000;
    private static final String PROMPT = """
            Расскажите о себе одним сообщением — по этому описанию я буду писать отклики:

            • чем занимаетесь и сколько лет опыта
            • стек / инструменты
            • 1–3 примера работ или ссылки на портфолио""";
    private static final String READY = """
            Готово! 🎉 Буду присылать новые заказы из выбранных категорий.
            Под каждым заказом есть кнопка «✍️ Отклик» — я подготовлю текст отклика.

            /categories — изменить категории
            /profile — изменить описание""";

    private final UserService userService;
    private final ProfileRepo profileRepo;
    private final MessageSender sender;

    @Override
    public String action() {
        return ACTION;
    }

    @Override
    public UserState state() {
        return UserState.FILLING_PROFILE;
    }

    public void askProfile(Long chatId, Long userId) {
        userService.setState(userId, UserState.FILLING_PROFILE);
        sender.send(new KeyboardMessage(chatId, PROMPT,
                Keyboard.keyboard(Keyboard.button("Пропустить", ACTION, "skip"))));
    }

    public void showProfile(Long chatId, Long userId) {
        String text = findAbout(userId)
                .map(about -> "<b>Ваш профиль:</b>\n\n" + Html.escape(about))
                .orElse("Профиль пока не заполнен — отклики будут общими.");
        sender.send(new KeyboardMessage(chatId, text,
                Keyboard.keyboard(Keyboard.button("✏️ Изменить", ACTION, "edit"))));
    }

    @Transactional(readOnly = true)
    public Optional<String> findAbout(Long userId) {
        return profileRepo.findById(userId).map(FreelancerProfile::getAbout);
    }

    /** Текст профиля от пользователя. */
    @Override
    public void handle(Message message) {
        Long userId = message.getFrom().getId();
        save(userId, Html.truncate(message.getText().strip(), MAX_ABOUT_LENGTH));
        userService.setState(userId, UserState.ACTIVE);
        sender.send(new TextMessage(message.getChatId(), READY));
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long userId = query.getFrom().getId();
        Long chatId = query.getMessage().getChatId();
        sender.send(new CallbackAnswer(chatId, query.getId(), null));

        switch (data.payload()) {
            case "edit" -> askProfile(chatId, userId);
            case "skip" -> {
                if (userService.getState(userId) != UserState.FILLING_PROFILE) {
                    return; // старая кнопка
                }
                userService.setState(userId, UserState.ACTIVE);
                sender.send(new EditMessage(chatId, query.getMessage().getMessageId(),
                        "Описание можно добавить позже командой /profile", null));
                sender.send(new TextMessage(chatId, READY));
            }
            default -> { }
        }
    }

    private void save(Long userId, String about) {
        FreelancerProfile profile = profileRepo.findById(userId).orElseGet(() -> {
            FreelancerProfile p = new FreelancerProfile();
            p.setUserId(userId);
            return p;
        });
        profile.setAbout(about);
        profile.setUpdatedAt(Instant.now());
        profileRepo.save(profile);
    }
}
