package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Сгенерированные отклики и журнал вызовов LLM. */
@Service
@RequiredArgsConstructor
public class AdminProposalService {

    public record ProposalRow(long id, long userId, String username, long kworkId, String kworkName, String text,
                              Instant createdAt, String model, Integer durationMs, Integer tokens) {}

    public record LlmCallRow(long id, Long userId, Long kworkId, String model, Instant startedAt, int durationMs,
                             String status, Integer promptTokens, Integer completionTokens, String error) {}

    private final JdbcClient jdbc;

    public PageResult<ProposalRow> list(Long userId, Long kworkId, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            where.append(" and p.user_id = :userId");
            params.put("userId", userId);
        }
        if (kworkId != null) {
            where.append(" and p.kwork_id = :kworkId");
            params.put("kworkId", kworkId);
        }
        long total = jdbc.sql("select count(*) from proposals p" + where).params(params).query(Long.class).single();
        List<ProposalRow> items = jdbc.sql("""
                        select p.id, p.user_id, u.username, p.kwork_id, k.kwork_name, p.text, p.created_at,
                               c.model, c.duration_ms, coalesce(c.prompt_tokens, 0) + coalesce(c.completion_tokens, 0) tokens
                        from proposals p
                        left join bot_users u on u.id = p.user_id
                        left join kwork k on k.kwork_order_id = p.kwork_id
                        left join llm_calls c on c.id = p.llm_call_id""" + where + " order by p.id desc limit :limit offset :offset")
                .params(params).param("limit", size).param("offset", page * size)
                .query((rs, i) -> new ProposalRow(rs.getLong("id"), rs.getLong("user_id"), rs.getString("username"),
                        rs.getLong("kwork_id"), rs.getString("kwork_name"), rs.getString("text"),
                        AdminUserService.instant(rs, "created_at"), rs.getString("model"),
                        (Integer) rs.getObject("duration_ms"), rs.getObject("model") == null ? null : rs.getInt("tokens")))
                .list();
        return new PageResult<>(items, total, page, size);
    }

    public PageResult<LlmCallRow> calls(String status, int page, int size) {
        String where = status == null || status.isBlank() ? "" : " where status = :status";
        Map<String, Object> params = status == null || status.isBlank() ? Map.of() : Map.of("status", status);
        long total = jdbc.sql("select count(*) from llm_calls" + where).params(params).query(Long.class).single();
        List<LlmCallRow> items = jdbc.sql("select * from llm_calls" + where + " order by id desc limit :limit offset :offset")
                .params(params).param("limit", size).param("offset", page * size)
                .query((rs, i) -> new LlmCallRow(rs.getLong("id"), (Long) rs.getObject("user_id"), (Long) rs.getObject("kwork_id"),
                        rs.getString("model"), AdminUserService.instant(rs, "started_at"), rs.getInt("duration_ms"),
                        rs.getString("status"), (Integer) rs.getObject("prompt_tokens"),
                        (Integer) rs.getObject("completion_tokens"), rs.getString("error")))
                .list();
        return new PageResult<>(items, total, page, size);
    }
}
