package org.example.telegramkworkbot.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/** Журнал вызовов языковой модели: длительность, токены, ошибки. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmCallRecorder {

    public record LlmCall(Long userId, Long kworkId, String model, Instant startedAt, long durationMs,
                          boolean ok, Integer promptTokens, Integer completionTokens, String error) {}

    private final JdbcClient jdbc;

    /** @return id записи или null, если записать не удалось */
    public Long record(LlmCall call) {
        try {
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("""
                            insert into llm_calls (user_id, kwork_id, model, started_at, duration_ms, status,
                                                   prompt_tokens, completion_tokens, error)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                    .params(call.userId(), call.kworkId(), call.model(), Timestamp.from(call.startedAt()),
                            (int) call.durationMs(), call.ok() ? "OK" : "ERROR",
                            call.promptTokens(), call.completionTokens(), call.error())
                    .update(keys, "id");
            Number id = keys.getKey();
            return id == null ? null : id.longValue();
        } catch (RuntimeException e) {
            log.warn("Не удалось записать вызов LLM: {}", e.getMessage());
            return null;
        }
    }
}
