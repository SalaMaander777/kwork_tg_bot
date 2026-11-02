package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.message.EditMessage;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.sender.SendStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Держит уже отправленные карточки актуальными: когда у заказа меняется число откликов
 * (парсер обновляет kwork.kwork_count) или заказ снимают с публикации, сообщение в чате редактируется.
 * Карточки, по которым пользователь уже открывал отклик, не трогаем — на их месте текст отклика.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bot.enabled", havingValue = "true", matchIfMissing = true)
public class CardRefresher {

    static final String CLOSED_MARK = "🔒 <b>Снят с публикации</b>\n";
    private static final long EDIT_PAUSE_MS = 50;
    private static final int MAX_FAILS = 3;

    private final JdbcClient jdbc;
    private final KworkRepo kworkRepo;
    private final KworkCardFormatter formatter;
    private final MessageSender sender;

    private final ReentrantLock lock = new ReentrantLock();

    /** Сколько часов после отправки карточка обновляется. */
    @Value("${kwork.card-refresh.ttl-hours:6}")
    private int ttlHours;
    /** Не чаще одного редактирования одного сообщения за этот срок, мин. */
    @Value("${kwork.card-refresh.min-gap-minutes:2}")
    private int minGapMinutes;
    @Value("${kwork.card-refresh.batch-size:200}")
    private int batchSize;

    private record Stale(long id, long userId, long kworkId, int messageId) {}

    @Scheduled(fixedDelayString = "${kwork.card-refresh.interval:PT1M}", initialDelayString = "PT30S")
    public void refresh() {
        if (!lock.tryLock()) {
            return;
        }
        try {
            int updated = 0;
            for (Stale card : findStale()) {
                if (refreshOne(card)) {
                    updated++;
                }
                pause();
            }
            if (updated > 0) {
                log.info("Обновлено карточек: {}", updated);
            }
        } catch (RuntimeException e) {
            log.error("Ошибка обновления карточек", e);
        } finally {
            lock.unlock();
        }
    }

    /** Карточки, у которых число откликов или статус в базе отличается от показанного в чате. */
    private java.util.List<Stale> findStale() {
        return jdbc.sql("""
                        select c.id, c.user_id, c.kwork_id, c.message_id
                        from card_messages c
                        join kwork k on k.kwork_order_id = c.kwork_id
                        where c.sent_at >= now() - make_interval(hours => :ttl)
                          and c.fail_count < :maxFails
                          and (c.refreshed_at is null or c.refreshed_at < now() - make_interval(mins => :gap))
                          and (k.kwork_count <> c.shown_count or k.is_active = c.shown_closed)
                          and not exists (select 1 from proposals p
                                          where p.user_id = c.user_id and p.kwork_id = c.kwork_id)
                        order by c.sent_at desc
                        limit :batch
                        """)
                .param("ttl", ttlHours).param("maxFails", MAX_FAILS)
                .param("gap", minGapMinutes).param("batch", batchSize)
                .query((rs, n) -> new Stale(rs.getLong("id"), rs.getLong("user_id"),
                        rs.getLong("kwork_id"), rs.getInt("message_id")))
                .list();
    }

    private boolean refreshOne(Stale card) {
        Optional<KworkEntity> found = kworkRepo.findById(card.kworkId());
        if (found.isEmpty()) {
            return false;
        }
        KworkEntity kwork = found.get();
        String text = (kwork.isActive() ? "" : CLOSED_MARK) + formatter.text(kwork);
        SendStatus status = sender.send(new EditMessage(card.userId(), card.messageId(), text,
                formatter.keyboard(kwork, Optional.empty())));
        switch (status) {
            case OK -> jdbc.sql("""
                            update card_messages set shown_count = :count, shown_closed = :closed,
                                   refreshed_at = now(), fail_count = 0 where id = :id
                            """)
                    .param("count", kwork.getKworkCount()).param("closed", !kwork.isActive())
                    .param("id", card.id()).update();
            case BLOCKED -> jdbc.sql("delete from card_messages where id = :id").param("id", card.id()).update();
            case FAILED -> jdbc.sql("""
                            update card_messages set fail_count = fail_count + 1, refreshed_at = now()
                            where id = :id
                            """).param("id", card.id()).update();
        }
        return status == SendStatus.OK;
    }

    private static void pause() {
        try {
            Thread.sleep(EDIT_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
