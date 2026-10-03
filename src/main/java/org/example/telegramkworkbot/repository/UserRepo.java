package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<UserEntity, Long> {
}
