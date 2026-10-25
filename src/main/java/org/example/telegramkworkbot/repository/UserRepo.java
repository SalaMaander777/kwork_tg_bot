package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepo extends JpaRepository<UserEntity, Long> {

    List<UserEntity> findAllByState(UserState state);
}
