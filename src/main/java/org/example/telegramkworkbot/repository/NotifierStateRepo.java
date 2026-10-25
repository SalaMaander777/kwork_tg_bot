package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.NotifierState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifierStateRepo extends JpaRepository<NotifierState, Integer> {
}
