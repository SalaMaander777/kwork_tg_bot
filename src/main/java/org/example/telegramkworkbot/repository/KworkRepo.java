package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KworkRepo extends JpaRepository<KworkEntity, Long>{

    List<KworkEntity> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    @Query("select coalesce(max(k.id), 0) from KworkEntity k")
    long findMaxId();
}
