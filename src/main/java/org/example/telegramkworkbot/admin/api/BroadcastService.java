package org.example.telegramkworkbot.admin.api;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.admin.AdminProperties;
import org.example.telegramkworkbot.admin.AuditService;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.sender.SendStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Рассылки сообщений пользователям. Выполняются по одной в отдельном потоке
 * с паузой между сообщениями (лимиты Telegram), прогресс пишется в таблицу broadcasts.
 */
@Slf4j
@Service
public class BroadcastService {

    /** Сегмент получателей. type: ALL | ACTIVE | CATEGORIES | INACTIVE */
    public record Segment(String type, List<Long> subcategoryIds, Integer inactiveDays) {}

    public record BroadcastRow(long id, String text, String segment, String status, int total, int sent, int failed,
                               Instant createdAt, Instant finishedAt) {}

    private static final long SEND_PAUSE_MS = 40;
    private static final int PROGRESS_EVERY = 20;

    private final JdbcClient jdbc;
    private final MessageSender sender;
    private final AdminProperties admin;
    private final AuditService audit;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "broadcast"));
    private final Set<Long> cancelled = ConcurrentHashMap.newKeySet();

    public BroadcastService(JdbcClient jdbc, MessageSender sender, AdminProperties admin, AuditService audit) {
        this.jdbc = jdbc;
        this.sender = sender;
        this.admin = admin;
        this.audit = audit;
        // рассылки, оборванные перезапуском приложения
        jdbc.sql("update broadcasts set status = 'FAILED', finished_at = now() where status = 'RUNNING'").update();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public long countRecipients(Segment segment) {
        Query q = recipientsQuery(segment);
        return jdbc.sql("select count(*) from (" + q.sql() + ") r").params(q.params()).query(Long.class).single();
    }

    public SendStatus sendTest(String text) {
        validate(text);
        if (admin.telegramId() == null) {
            throw new IllegalArgumentException("Не задан ADMIN_TELEGRAM_ID — некому отправить тест");
        }
        return sender.send(new KeyboardMessage(admin.telegramId(), text, null));
    }

    public long start(String text, Segment segment) {
        validate(text);
        Query q = recipientsQuery(segment);
        List<Long> recipients = jdbc.sql(q.sql()).params(q.params()).query(Long.class).list();
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("В сегменте нет получателей");
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into broadcasts (text, segment, status, total, created_at) values (?, ?, 'RUNNING', ?, ?)")
                .params(text, describe(segment), recipients.size(), Timestamp.from(Instant.now()))
                .update(keys, "id");
        long id = keys.getKey().longValue();
        audit.record("BROADCAST_START", String.valueOf(id), describe(segment) + ", получателей: " + recipients.size());
        executor.submit(() -> run(id, text, recipients));
        return id;
    }

    public void cancel(long id) {
        cancelled.add(id);
        jdbc.sql("update broadcasts set status = 'CANCELLED', finished_at = now() where id = ? and status = 'RUNNING'")
                .param(id).update();
        audit.record("BROADCAST_CANCEL", String.valueOf(id), null);
    }

    public List<BroadcastRow> list() {
        return jdbc.sql("select * from broadcasts order by id desc limit 50").query(this::row).list();
    }

    public BroadcastRow get(long id) {
        return jdbc.sql("select * from broadcasts where id = ?").param(id).query(this::row).optional()
                .orElseThrow(() -> new NoSuchElementException("Рассылка не найдена"));
    }

    private void run(long id, String text, List<Long> recipients) {
        int sent = 0;
        int failed = 0;
        String status = "DONE";
        try {
            for (int i = 0; i < recipients.size(); i++) {
                if (cancelled.contains(id) || Thread.currentThread().isInterrupted()) {
                    status = "CANCELLED";
                    break;
                }
                Long userId = recipients.get(i);
                SendStatus result = sender.send(new KeyboardMessage(userId, text, null));
                if (result == SendStatus.OK) {
                    sent++;
                } else {
                    failed++;
                    if (result == SendStatus.BLOCKED) {
                        jdbc.sql("update bot_users set state = 'BLOCKED' where id = ?").param(userId).update();
                    }
                }
                if ((i + 1) % PROGRESS_EVERY == 0) {
                    progress(id, sent, failed);
                }
                Thread.sleep(SEND_PAUSE_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "CANCELLED";
        } catch (RuntimeException e) {
            log.error("Рассылка {} прервана", id, e);
            status = "FAILED";
        } finally {
            cancelled.remove(id);
            jdbc.sql("update broadcasts set sent = ?, failed = ?, status = ?, finished_at = now() where id = ?")
                    .params(sent, failed, status, id).update();
            log.info("Рассылка {} завершена: {}, отправлено {}, ошибок {}", id, status, sent, failed);
        }
    }

    private void progress(long id, int sent, int failed) {
        jdbc.sql("update broadcasts set sent = ?, failed = ? where id = ?").params(sent, failed, id).update();
    }

    private record Query(String sql, Map<String, Object> params) {}

    /** Получатели: не забанены и не заблокировали бота. */
    private Query recipientsQuery(Segment segment) {
        String type = segment == null || segment.type() == null ? "ALL" : segment.type();
        StringBuilder sql = new StringBuilder("select u.id from bot_users u where not u.banned and u.state <> 'BLOCKED'");
        Map<String, Object> params = new HashMap<>();
        switch (type) {
            case "ALL" -> { }
            case "ACTIVE" -> sql.append(" and u.state = 'ACTIVE'");
            case "CATEGORIES" -> {
                if (segment.subcategoryIds() == null || segment.subcategoryIds().isEmpty()) {
                    throw new IllegalArgumentException("Выберите хотя бы одну подкатегорию");
                }
                sql.append(" and exists (select 1 from user_subcategories s where s.user_id = u.id and s.subcategory_id in (:subs))");
                params.put("subs", segment.subcategoryIds());
            }
            case "INACTIVE" -> {
                int days = segment.inactiveDays() == null ? 7 : segment.inactiveDays();
                if (days < 1 || days > 365) {
                    throw new IllegalArgumentException("Дней неактивности: от 1 до 365");
                }
                sql.append(" and (u.last_activity_at is null or u.last_activity_at < now() - make_interval(days => :days))");
                params.put("days", days);
            }
            default -> throw new IllegalArgumentException("Неизвестный сегмент: " + type);
        }
        return new Query(sql + " order by u.id", params);
    }

    private static String describe(Segment segment) {
        String type = segment == null || segment.type() == null ? "ALL" : segment.type();
        return switch (type) {
            case "ACTIVE" -> "Активные";
            case "CATEGORIES" -> "Подкатегории " + segment.subcategoryIds();
            case "INACTIVE" -> "Неактивные " + (segment.inactiveDays() == null ? 7 : segment.inactiveDays()) + " дн.";
            default -> "Все";
        };
    }

    private static void validate(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст рассылки пуст");
        }
        if (text.length() > 4000) {
            throw new IllegalArgumentException("Текст не длиннее 4000 символов");
        }
    }

    private BroadcastRow row(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new BroadcastRow(rs.getLong("id"), rs.getString("text"), rs.getString("segment"), rs.getString("status"),
                rs.getInt("total"), rs.getInt("sent"), rs.getInt("failed"),
                AdminUserService.instant(rs, "created_at"), AdminUserService.instant(rs, "finished_at"));
    }
}
