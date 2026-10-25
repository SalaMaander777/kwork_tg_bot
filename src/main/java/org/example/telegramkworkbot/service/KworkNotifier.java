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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Периодически забирает новые заказы, которые парсер положил в таблицу kwork,
 * и рассылает их пользователям с подходящими подкатегориями.
 * Курсор — id последнего обработанного заказа (notifier_state).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KworkNotifier {

    // Telegram допускает ~30 сообщений в секунду суммарно
    private static final long SEND_PAUSE_MS = 40;

    private final KworkRepo kworkRepo;
    private final NotifierStateRepo stateRepo;
    private final UserService userService;
    private final KworkCardFormatter formatter;
    private final MessageSender sender;

    @Value("${kwork.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${kwork.poll-interval:PT1M}", initialDelayString = "PT10S")
    public void poll() {
        NotifierState state = stateRepo.findById(NotifierState.SINGLETON_ID).orElseGet(this::initState);

        List<KworkEntity> fresh = kworkRepo.findByIdGreaterThanOrderByIdAsc(
                state.getLastKworkId(), PageRequest.of(0, batchSize));
        if (fresh.isEmpty()) {
            return;
        }

        Map<Long, List<Long>> subscribers = subscribersByCategory(userService.findActive());
        Set<Long> blocked = new HashSet<>();
        int sent = 0;
        for (KworkEntity kwork : fresh) {
            for (Long userId : subscribers.getOrDefault(kwork.getCategoryId(), List.of())) {
                if (blocked.contains(userId)) {
                    continue;
                }
                SendStatus status = sender.send(new KeyboardMessage(userId, formatter.text(kwork), formatter.keyboard(kwork, Optional.empty())));
                if (status == SendStatus.BLOCKED) {
                    blocked.add(userId);
                    userService.setState(userId, UserState.BLOCKED);
                } else if (status == SendStatus.OK) {
                    sent++;
                }
                pause();
            }
        }

        state.setLastKworkId(fresh.get(fresh.size() - 1).getId());
        stateRepo.save(state);
        log.info("Новых заказов: {}, отправлено карточек: {}", fresh.size(), sent);
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

    /** Первый запуск: не рассылаем старые заказы, начинаем с текущего максимума. */
    private NotifierState initState() {
        NotifierState state = new NotifierState(kworkRepo.findMaxId());
        log.info("Курсор рассылки инициализирован: {}", state.getLastKworkId());
        return stateRepo.save(state);
    }

    private static void pause() {
        try {
            Thread.sleep(SEND_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
