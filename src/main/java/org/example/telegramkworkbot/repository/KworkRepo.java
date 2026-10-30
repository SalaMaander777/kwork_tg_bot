package org.example.telegramkworkbot.repository;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KworkRepo extends JpaRepository<KworkEntity, Long>{

    /**
     * Новые заказы для рассылки: только те, что ещё есть на сайте и появились у парсера
     * не раньше чем maxAgeMinutes назад (после простоя бота старьё не рассылаем).
     */
    @Query(value = """
            select * from kwork
            where kwork_order_id > :afterId
              and is_active
              and first_seen_at >= now() - make_interval(mins => :maxAgeMinutes)
            order by kwork_order_id
            limit :limit
            """, nativeQuery = true)
    List<KworkEntity> findDeliverable(@Param("afterId") long afterId,
                                      @Param("maxAgeMinutes") int maxAgeMinutes,
                                      @Param("limit") int limit);

    @Query("select coalesce(max(k.id), 0) from KworkEntity k")
    long findMaxId();
}
