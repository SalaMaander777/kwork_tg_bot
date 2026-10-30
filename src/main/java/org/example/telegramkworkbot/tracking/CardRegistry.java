package org.example.telegramkworkbot.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Учёт отправленных карточек заказов (таблица card_messages) для их последующего обновления. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardRegistry {

    public record Card(Long userId, Long kworkId, Integer messageId, int shownCount, Instant sentAt) {}

    private final JdbcTemplate jdbc;

    public void recordAll(List<Card> cards) {
        if (cards.isEmpty()) {
            return;
        }
        try {
            jdbc.batchUpdate("""
                    insert into card_messages (user_id, kwork_id, message_id, shown_count, sent_at)
                    values (?, ?, ?, ?, ?)
                    on conflict (user_id, kwork_id) do update set
                        message_id = excluded.message_id, shown_count = excluded.shown_count,
                        shown_closed = false, fail_count = 0, sent_at = excluded.sent_at, refreshed_at = null
                    """, cards, cards.size(), (ps, c) -> {
                ps.setLong(1, c.userId());
                ps.setLong(2, c.kworkId());
                ps.setInt(3, c.messageId());
                ps.setInt(4, c.shownCount());
                ps.setTimestamp(5, Timestamp.from(c.sentAt()));
            });
        } catch (RuntimeException e) {
            log.warn("Не удалось записать {} карточек: {}", cards.size(), e.getMessage());
        }
    }

    /** Пользователь скрыл карточку — обновлять в ней больше нечего. */
    public void forgetMessage(Long chatId, Integer messageId) {
        // для лички chatId == userId
        jdbc.update("delete from card_messages where user_id = ? and message_id = ?", chatId, messageId);
    }
}
