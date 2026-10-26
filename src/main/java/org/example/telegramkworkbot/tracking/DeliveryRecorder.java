package org.example.telegramkworkbot.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.sender.SendStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Журнал отправленных карточек заказов. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryRecorder {

    public record Delivery(Long userId, Long kworkId, Instant sentAt, SendStatus status) {}

    private final JdbcTemplate jdbc;

    public void recordAll(List<Delivery> deliveries) {
        if (deliveries.isEmpty()) {
            return;
        }
        try {
            jdbc.batchUpdate("insert into deliveries (user_id, kwork_id, sent_at, status) values (?, ?, ?, ?)",
                    deliveries, deliveries.size(), (ps, d) -> {
                        ps.setLong(1, d.userId());
                        ps.setLong(2, d.kworkId());
                        ps.setTimestamp(3, Timestamp.from(d.sentAt()));
                        ps.setString(4, d.status().name());
                    });
        } catch (RuntimeException e) {
            log.warn("Не удалось записать {} доставок: {}", deliveries.size(), e.getMessage());
        }
    }
}
