package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface KworkRepo extends JpaRepository<KworkEntity, Long>{

    /**
     * Новые заказы для рассылки после курсора (afterActivatedAt, afterId): только те, что ещё есть на сайте
     * и появились (или были подняты) не раньше чем maxAgeMinutes назад - после простоя бота старьё не рассылаем.
     * Порядок и курсор по времени появления, а не по id: заказы публикуются после модерации не по порядку id,
     * а старые заказы поднимаются заново.
     */
    @Query(value = """
            select * from kwork
            where (activated_at, kwork_order_id) > (:afterActivatedAt, :afterId)
              and is_active
              and activated_at >= now() - make_interval(mins => :maxAgeMinutes)
            order by activated_at, kwork_order_id
            limit :limit
            """, nativeQuery = true)
    List<KworkEntity> findDeliverable(@Param("afterActivatedAt") Instant afterActivatedAt,
                                      @Param("afterId") long afterId,
                                      @Param("maxAgeMinutes") int maxAgeMinutes,
                                      @Param("limit") int limit);

    @Query("select coalesce(max(k.id), 0) from KworkEntity k")
    long findMaxId();
}
