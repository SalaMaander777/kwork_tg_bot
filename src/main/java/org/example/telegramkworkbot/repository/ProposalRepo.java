package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ProposalRepo extends JpaRepository<Proposal, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);
}
