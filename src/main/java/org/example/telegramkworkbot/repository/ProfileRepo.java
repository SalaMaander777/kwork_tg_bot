package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.FreelancerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepo extends JpaRepository<FreelancerProfile, Long> {
}
