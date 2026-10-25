package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProposalRepo extends JpaRepository<Proposal, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);

    /** Все варианты отклика пользователя на заказ, от первого к последнему. */
    List<Proposal> findByUserIdAndKworkIdOrderByIdAsc(Long userId, Long kworkId);

    Optional<Proposal> findFirstByUserIdAndKworkIdOrderByIdDesc(Long userId, Long kworkId);
}
