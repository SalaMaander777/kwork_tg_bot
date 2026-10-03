package org.example.telegramkworkbot.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;

    @Transactional
    public boolean registerOrUpdateUser(User tgUser){
        UserEntity localUser = userRepo.findById(tgUser.getId()).orElse(null);
        boolean isNew = localUser == null;
        if (isNew){
            localUser = new UserEntity();
            localUser.setId(tgUser.getId());
            localUser.setCreatedAt(Instant.now());
        }
        localUser.setFirstName(tgUser.getFirstName());
        localUser.setUsername(tgUser.getUserName());
        localUser.setLastName(tgUser.getLastName());
        localUser.setState(UserState.NEW);
        userRepo.save(localUser);
        return isNew;



    }

}
