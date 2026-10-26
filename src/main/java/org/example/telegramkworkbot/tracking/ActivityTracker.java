package org.example.telegramkworkbot.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Последняя активность пользователей (для DAU/WAU/MAU) и список забаненных.
 * В БД пишем не чаще раза в 5 минут на пользователя; бан-лист держим в памяти.
 */
@Slf4j
@Component
public class ActivityTracker {

    private static final Duration WRITE_INTERVAL = Duration.ofMinutes(5);

    private final JdbcClient jdbc;
    private final Map<Long, Instant> lastWrites = new ConcurrentHashMap<>();
    private final Set<Long> banned = ConcurrentHashMap.newKeySet();

    public ActivityTracker(JdbcClient jdbc) {
        this.jdbc = jdbc;
        banned.addAll(jdbc.sql("select id from bot_users where banned").query(Long.class).list());
    }

    public void touch(Long userId) {
        Instant now = Instant.now();
        Instant last = lastWrites.get(userId);
        if (last != null && last.plus(WRITE_INTERVAL).isAfter(now)) {
            return;
        }
        lastWrites.put(userId, now);
        try {
            jdbc.sql("update bot_users set last_activity_at = ? where id = ?")
                    .params(Timestamp.from(now), userId)
                    .update();
        } catch (RuntimeException e) {
            log.warn("Не удалось обновить активность {}: {}", userId, e.getMessage());
        }
    }

    public boolean isBanned(Long userId) {
        return banned.contains(userId);
    }

    public void setBanned(Long userId, boolean value) {
        if (value) {
            banned.add(userId);
        } else {
            banned.remove(userId);
        }
    }
}
