package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KworkRepo extends JpaRepository<KworkEntity, Long>{
}
