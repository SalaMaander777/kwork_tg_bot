package org.example.telegramkworkbot.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.repository.UserRepo;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class StateService {

    private final UserRepo userRepo;


    public UserState getState(Long userId){
        return userRepo.findById(userId)
                .orElseThrow(() ->
                    new IllegalStateException(
                            "User not found" + userId
                    ))
                .getState();
    }

    public void setState(Long userId, UserState state){
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User not found" + userId
                        ));


        user.setState(state);
        userRepo.save(user);
    }
}
