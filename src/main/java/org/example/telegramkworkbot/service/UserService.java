package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    /**
     * Создаёт пользователя или обновляет его имя/username.
     * Состояние существующего пользователя не трогает.
     *
     * @return true, если пользователь новый
     */
    @Transactional
    public boolean registerOrUpdateUser(User tgUser) {
        Optional<UserEntity> existing = userRepo.findById(tgUser.getId());
        UserEntity user = existing.orElseGet(() -> newUser(tgUser.getId()));

        user.setUsername(tgUser.getUserName());
        user.setFirstName(tgUser.getFirstName());
        user.setLastName(tgUser.getLastName());
        userRepo.save(user);

        return existing.isEmpty();
    }

    @Transactional(readOnly = true)
    public UserState getState(Long userId) {
        return findOrThrow(userId).getState();
    }

    @Transactional
    public void setState(Long userId, UserState state) {
        // save() не нужен: сущность управляется Hibernate внутри транзакции,
        // изменение запишется при коммите
        findOrThrow(userId).setState(state);
    }

    private UserEntity findOrThrow(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    private UserEntity newUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setCreatedAt(Instant.now());
        return user; // state по умолчанию NEW задан в самой сущности
    }
}