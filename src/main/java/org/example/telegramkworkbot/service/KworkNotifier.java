package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.entities.NotifierState;
import org.example.telegramkworkbot.entities.UserEntity;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.repository.NotifierStateRepo;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.sender.SendStatus;
import org.example.telegramkworkbot.settings.SettingsService;
import org.example.telegramkworkbot.tracking.ActivityTracker;
import org.example.telegramkworkbot.tracking.DeliveryRecorder;
import org.example.telegramkworkbot.tracking.DeliveryRecorder.Delivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Периодически забирает новые заказы, которые парсер положил в таблицу kwork,
 * и рассылает их пользователям с подходящими подкатегориями.
 * Курсор — id последнего обработанного заказа (notifier_state).
 * Интервал, размер пачки и пауза берутся из настроек на каждом проходе.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KworkNotifier implements SchedulingConfigurer {

    // Telegram допускает ~30 сообщений в секунду суммарно
    static final long SEND_PAUSE_MS = 40;

    private final KworkRepo kworkRepo;
    private final NotifierStateRepo stateRepo;
    private final UserService userService;
    private final KworkCardFormatter formatter;
    private final MessageSender sender;
    private final SettingsService settings;
    private final ActivityTracker activity;
    private final DeliveryRecorder deliveries;

    private final ReentrantLock lock = new ReentrantLock();

    // без подключения к Telegram (BOT_ENABLED=false) рассылать нельзя: карточки не дойдут, а курсор сдвинется
    @Value("${bot.enabled:true}")
    private boolean botEnabled;

    // заказ, который парсер впервые увидел раньше этого срока, не рассылается (бот простаивал)
    @Value("${kwork.max-age:PT3H}")
    private Duration maxAge;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        if (!botEnabled) {
            log.warn("Бот отключён (bot.enabled=false) — рассылка заказов не запускается");
            return;
        }
        registrar.addTriggerTask(this::poll, context -> {
            Instant last = context.lastCompletion();
            return last == null
                    ? Instant.now().plusSeconds(10)
                    : last.plus(settings.pollInterval());
        });
    }

    /** @return сколько новых заказов обработано; -1, если проход уже идёт */
    public int poll() {
        if (!botEnabled) {
            throw new IllegalStateException("Бот отключён (BOT_ENABLED=false), рассылка невозможна");
        }
        if (!lock.tryLock()) {
            return -1;
        }
        try {
            return doPoll();
        } catch (RuntimeException e) {
            log.error("Ошибка рассылки заказов", e);
            return 0;
        } finally {
            lock.unlock();
        }
    }

    private int doPoll() {
        NotifierState state = stateRepo.findById(NotifierState.SINGLETON_ID).orElseGet(this::initState);
        state.setLastRunAt(Instant.now());

        List<KworkEntity> fresh = kworkRepo.findDeliverable(
                state.getLastKworkId(), (int) maxAge.toMinutes(), settings.batchSize());
        if (fresh.isEmpty()) {
            stateRepo.save(state);
            return 0;
        }

        int sent = 0;
        if (settings.notifierEnabled()) {
            sent = deliver(fresh);
        } else {
            log.info("Рассылка на паузе, пропущено заказов: {}", fresh.size());
        }

        state.setLastKworkId(fresh.get(fresh.size() - 1).getId());
        stateRepo.save(state);
        log.info("Новых заказов: {}, отправлено карточек: {}", fresh.size(), sent);
        return fresh.size();
    }

    private int deliver(List<KworkEntity> fresh) {
        List<UserEntity> users = userService.findActive().stream()
                .filter(user -> !activity.isBanned(user.getId()))
                .toList();
        Map<Long, List<Long>> subscribers = subscribersByCategory(users);
        Set<Long> blocked = new HashSet<>();
        List<Delivery> journal = new ArrayList<>();
        int sent = 0;
        for (KworkEntity kwork : fresh) {
            for (Long userId : subscribers.getOrDefault(kwork.getCategoryId(), List.of())) {
                if (blocked.contains(userId)) {
                    continue;
                }
                SendStatus status = sender.send(new KeyboardMessage(userId, formatter.text(kwork), formatter.keyboard(kwork, Optional.empty())));
                journal.add(new Delivery(userId, kwork.getId(), Instant.now(), status));
                if (status == SendStatus.BLOCKED) {
                    blocked.add(userId);
                    userService.setState(userId, UserState.BLOCKED);
                } else if (status == SendStatus.OK) {
                    sent++;
                }
                pause();
            }
        }
        deliveries.recordAll(journal);
        return sent;
    }

    /** id подкатегории -> пользователи, которые на неё подписаны. Для лички chatId == userId. */
    static Map<Long, List<Long>> subscribersByCategory(Collection<UserEntity> users) {
        Map<Long, List<Long>> result = new HashMap<>();
        for (UserEntity user : users) {
            for (Long subId : user.getSubcategoryIds()) {
                result.computeIfAbsent(subId, k -> new ArrayList<>()).add(user.getId());
            }
        }
        return result;
    }

    /** Сдвинуть курсор на текущий максимум: всё, что уже есть в базе, рассылаться не будет. */
    public long resetCursor() {
        NotifierState state = stateRepo.findById(NotifierState.SINGLETON_ID).orElseGet(this::initState);
        state.setLastKworkId(kworkRepo.findMaxId());
        stateRepo.save(state);
        return state.getLastKworkId();
    }

    /** Первый запуск: не рассылаем старые заказы, начинаем с текущего максимума. */
    private NotifierState initState() {
        NotifierState state = new NotifierState(kworkRepo.findMaxId());
        log.info("Курсор рассылки инициализирован: {}", state.getLastKworkId());
        return stateRepo.save(state);
    }

    static void pause() {
        try {
            Thread.sleep(SEND_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
