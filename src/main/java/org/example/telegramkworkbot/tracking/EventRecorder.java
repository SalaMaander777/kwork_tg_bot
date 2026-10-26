package org.example.telegramkworkbot.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/** Пишет события пользователей для метрик. Ошибки записи не должны ломать работу бота. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRecorder {

    private final JdbcClient jdbc;

    public void record(Long userId, EventType type) {
        record(userId, type, null);
    }

    public void record(Long userId, EventType type, String payload) {
        try {
            jdbc.sql("insert into bot_events (user_id, type, created_at, payload) values (?, ?, ?, ?)")
                    .params(userId, type.name(), Timestamp.from(Instant.now()), payload)
                    .update();
        } catch (RuntimeException e) {
            log.warn("Не удалось записать событие {} для {}: {}", type, userId, e.getMessage());
        }
    }
}
