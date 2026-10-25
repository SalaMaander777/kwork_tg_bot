package org.example.telegramkworkbot.admin.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Защита от перебора пароля: после MAX_FAILURES неудач подряд IP блокируется на BLOCK_DURATION. */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant blockedUntil) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    /** @return сколько ещё ждать, если IP заблокирован; иначе null */
    public Duration blockedFor(String ip) {
        Attempts a = attempts.get(ip);
        if (a == null || a.blockedUntil() == null) {
            return null;
        }
        Duration left = Duration.between(Instant.now(), a.blockedUntil());
        if (left.isNegative()) {
            attempts.remove(ip);
            return null;
        }
        return left;
    }

    public void failed(String ip) {
        attempts.compute(ip, (key, a) -> {
            int failures = (a == null ? 0 : a.failures()) + 1;
            Instant blockedUntil = failures >= MAX_FAILURES ? Instant.now().plus(BLOCK_DURATION) : null;
            return new Attempts(failures, blockedUntil);
        });
    }

    public void succeeded(String ip) {
        attempts.remove(ip);
    }
}
