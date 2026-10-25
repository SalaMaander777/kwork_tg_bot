package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    /**
     * Создаёт пользователя или обновляет его имя/username.
     * Состояние существующего пользователя не трогает.
     */
    @Transactional
    public UserEntity registerOrUpdateUser(User tgUser) {
        Optional<UserEntity> existing = userRepo.findById(tgUser.getId());
        UserEntity user = existing.orElseGet(() -> newUser(tgUser.getId()));

        user.setUsername(tgUser.getUserName());
        user.setFirstName(tgUser.getFirstName());
        user.setLastName(tgUser.getLastName());
        userRepo.save(user);
        return user;
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

    @Transactional(readOnly = true)
    public Optional<UserState> findState(Long userId) {
        return userRepo.findById(userId).map(UserEntity::getState);
    }

    @Transactional(readOnly = true)
    public Set<Long> getSubcategories(Long userId) {
        return new HashSet<>(findOrThrow(userId).getSubcategoryIds());
    }

    @Transactional
    public void toggleSubcategory(Long userId, Long subcategoryId) {
        Set<Long> ids = findOrThrow(userId).getSubcategoryIds();
        if (!ids.remove(subcategoryId)) {
            ids.add(subcategoryId);
        }
    }

    /** Если выбраны все переданные подкатегории — снимает их, иначе выбирает все. */
    @Transactional
    public void toggleAll(Long userId, Collection<Long> subcategoryIds) {
        Set<Long> ids = findOrThrow(userId).getSubcategoryIds();
        if (ids.containsAll(subcategoryIds)) {
            ids.removeAll(subcategoryIds);
        } else {
            ids.addAll(subcategoryIds);
        }
    }

    /** Активные пользователи вместе с выбранными подкатегориями (коллекция EAGER). */
    @Transactional(readOnly = true)
    public List<UserEntity> findActive() {
        return userRepo.findAllByState(UserState.ACTIVE);
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