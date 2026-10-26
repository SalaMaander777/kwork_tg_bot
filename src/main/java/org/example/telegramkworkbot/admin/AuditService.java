package org.example.telegramkworkbot.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.sql.Timestamp;
import java.time.Instant;

/** Журнал действий администратора. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final JdbcClient jdbc;

    public void record(String action, String target, String details) {
        log.info("Админ: {} {} {}", action, target, details == null ? "" : details);
        jdbc.sql("insert into admin_audit (action, target, details, ip, created_at) values (?, ?, ?, ?, ?)")
                .params(action, target, details, currentIp(), Timestamp.from(Instant.now()))
                .update();
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
