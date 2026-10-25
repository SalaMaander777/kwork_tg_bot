package org.example.telegramkworkbot.admin.api;

import org.example.telegramkworkbot.dto.KworkCategoryDto;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.example.telegramkworkbot.entities.NotifierState;
import org.example.telegramkworkbot.service.CategoryCatalog;
import org.example.telegramkworkbot.settings.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Метрики для дашборда. Всё считается SQL-запросами по таблицам бота и парсера.
 * Бакеты времени — в часовом поясе admin.timezone. kwork.creation_date хранится без пояса (время МСК).
 */
@Service
public class MetricsService {

    public record Kpi(double value, double previous) {}

    public record Point(LocalDateTime t, double value) {}

    public record FunnelStep(String key, String label, long count) {}

    public record CategoryStat(long id, String name, String root, long orders, long subscribers, long proposals) {}

    public record HeatCell(int dow, int hour, long count) {}

    public record ModelTokens(String model, long calls, long promptTokens, long completionTokens) {}

    public record Tokens(List<ModelTokens> byModel, long periodTotal, long allTimeTotal, long budget) {}

    public record Health(String status, String title, String detail, Map<String, Object> data) {}

    /** Метрика временного ряда: SQL выбирает (t — бакет, v — значение) за период [:from, :to). */
    private static final Map<String, String> SERIES = Map.of(
            "users_new", """
                    select date_trunc(cast(:bucket as text), created_at at time zone cast(:tz as text)) t, count(*) v
                    from bot_users where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz) group by 1""",
            "active_users", """
                    select date_trunc(cast(:bucket as text), created_at at time zone cast(:tz as text)) t, count(distinct user_id) v
                    from bot_events where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz) group by 1""",
            "orders", """
                    select date_trunc(cast(:bucket as text), creation_date) t, count(*) v
                    from kwork where creation_date >= (cast(:from as timestamptz) at time zone cast(:tz as text)) and creation_date < (cast(:to as timestamptz) at time zone cast(:tz as text)) group by 1""",
            "deliveries", """
                    select date_trunc(cast(:bucket as text), sent_at at time zone cast(:tz as text)) t, count(*) v
                    from deliveries where status = 'OK' and sent_at >= cast(:from as timestamptz) and sent_at < cast(:to as timestamptz) group by 1""",
            "proposals", """
                    select date_trunc(cast(:bucket as text), created_at at time zone cast(:tz as text)) t, count(*) v
                    from proposals where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz) group by 1""",
            "llm_errors", """
                    select date_trunc(cast(:bucket as text), started_at at time zone cast(:tz as text)) t, count(*) v
                    from llm_calls where status = 'ERROR' and started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz) group by 1""",
            "llm_latency", """
                    select date_trunc(cast(:bucket as text), started_at at time zone cast(:tz as text)) t, avg(duration_ms) v
                    from llm_calls where status = 'OK' and started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz) group by 1""",
            "tokens", """
                    select date_trunc(cast(:bucket as text), started_at at time zone cast(:tz as text)) t,
                           sum(coalesce(prompt_tokens, 0) + coalesce(completion_tokens, 0)) v
                    from llm_calls where started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz) group by 1""");

    private final JdbcClient jdbc;
    private final CategoryCatalog catalog;
    private final SettingsService settings;
    private final String timezone;

    public MetricsService(JdbcClient jdbc, CategoryCatalog catalog, SettingsService settings,
                          @Value("${admin.timezone:Europe/Moscow}") String timezone) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.settings = settings;
        this.timezone = ZoneId.of(timezone).getId();
    }

    public Map<String, Kpi> summary(Period period) {
        Map<String, Double> now = periodKpis(period);
        Map<String, Double> before = periodKpis(period.previous());
        Map<String, Kpi> result = new LinkedHashMap<>();
        now.forEach((key, value) -> result.put(key, new Kpi(value, before.getOrDefault(key, 0.0))));

        // срезы «на сейчас» — без сравнения
        jdbc.sql("""
                        select count(*) total,
                               count(*) filter (where state = 'ACTIVE' and not banned) active,
                               count(*) filter (where state = 'BLOCKED') blocked,
                               count(*) filter (where banned) banned,
                               count(*) filter (where last_activity_at >= now() - interval '1 day') dau,
                               count(*) filter (where last_activity_at >= now() - interval '7 days') wau,
                               count(*) filter (where last_activity_at >= now() - interval '30 days') mau
                        from bot_users""")
                .query((rs, i) -> {
                    for (String key : List.of("total", "active", "blocked", "banned", "dau", "wau", "mau")) {
                        double v = rs.getLong(key);
                        result.put("users_" + key, new Kpi(v, v));
                    }
                    return null;
                })
                .list();
        return result;
    }

    private Map<String, Double> periodKpis(Period p) {
        return jdbc.sql("""
                        select
                          (select count(*) from bot_users where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz)) users_new,
                          (select count(*) from kwork where creation_date >= (cast(:from as timestamptz) at time zone cast(:tz as text))
                                                       and creation_date < (cast(:to as timestamptz) at time zone cast(:tz as text))) orders,
                          (select count(*) from deliveries where status = 'OK' and sent_at >= cast(:from as timestamptz) and sent_at < cast(:to as timestamptz)) deliveries,
                          (select count(*) from proposals where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz)) proposals,
                          (select count(distinct (user_id, kwork_id)) from proposals
                             where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz)) proposal_orders,
                          (select count(*) from llm_calls where status = 'ERROR' and started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz)) llm_errors,
                          (select count(*) from llm_calls where started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz)) llm_calls,
                          (select coalesce(avg(duration_ms), 0) from llm_calls
                             where status = 'OK' and started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz)) llm_latency,
                          (select coalesce(sum(coalesce(prompt_tokens, 0) + coalesce(completion_tokens, 0)), 0)
                             from llm_calls where started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz)) tokens,
                          (select count(distinct user_id) from bot_events where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz)) active_users
                        """)
                .param("from", p.fromTs()).param("to", p.toTs()).param("tz", timezone)
                .query((rs, i) -> {
                    Map<String, Double> m = new LinkedHashMap<>();
                    for (String key : List.of("users_new", "orders", "deliveries", "proposals", "llm_errors",
                            "llm_calls", "llm_latency", "tokens", "active_users")) {
                        m.put(key, rs.getDouble(key));
                    }
                    double deliveries = rs.getDouble("deliveries");
                    m.put("conversion", deliveries == 0 ? 0 : rs.getDouble("proposal_orders") / deliveries * 100);
                    return m;
                })
                .single();
    }

    public List<Point> series(String metric, Period period, String bucket) {
        String sql = SERIES.get(metric);
        if (sql == null) {
            throw new IllegalArgumentException("Неизвестная метрика: " + metric);
        }
        String b = bucket == null ? period.defaultBucket() : bucket;
        if (!b.equals("hour") && !b.equals("day")) {
            throw new IllegalArgumentException("bucket: hour или day");
        }
        if (b.equals("hour") && Duration.between(period.from(), period.to()).toDays() > 14) {
            b = "day"; // слишком много точек
        }
        return jdbc.sql("""
                        with buckets as (
                          select generate_series(date_trunc(cast(:bucket as text), cast(:from as timestamptz) at time zone cast(:tz as text)),
                                                 date_trunc(cast(:bucket as text), (cast(:to as timestamptz) - interval '1 microsecond') at time zone cast(:tz as text)),
                                                 cast('1 ' || cast(:bucket as text) as interval)) t
                        ), data as (""" + sql + """
                        )
                        select b.t, coalesce(d.v, 0) v from buckets b left join data d on d.t = b.t order by b.t""")
                .param("bucket", b).param("from", period.fromTs()).param("to", period.toTs()).param("tz", timezone)
                .query((rs, i) -> new Point(rs.getTimestamp("t").toLocalDateTime(), rs.getDouble("v")))
                .list();
    }

    /** Воронка по пользователям, пришедшим за период. */
    public List<FunnelStep> funnel(Period period) {
        return jdbc.sql("""
                        with cohort as (select * from bot_users where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz))
                        select
                          (select count(*) from cohort) started,
                          (select count(*) from cohort c where exists (select 1 from user_subcategories s where s.user_id = c.id)) categories,
                          (select count(*) from cohort c where exists (select 1 from freelancer_profiles f where f.user_id = c.id)) profile,
                          (select count(*) from cohort c where c.state in ('ACTIVE', 'BLOCKED')) activated,
                          (select count(*) from cohort c where exists (select 1 from deliveries d where d.user_id = c.id and d.status = 'OK')) got_order,
                          (select count(*) from cohort c where exists (select 1 from proposals p where p.user_id = c.id)) proposal
                        """)
                .param("from", period.fromTs()).param("to", period.toTs())
                .query((rs, i) -> List.of(
                        new FunnelStep("started", "Нажали /start", rs.getLong("started")),
                        new FunnelStep("categories", "Выбрали категории", rs.getLong("categories")),
                        new FunnelStep("profile", "Заполнили профиль", rs.getLong("profile")),
                        new FunnelStep("activated", "Завершили онбординг", rs.getLong("activated")),
                        new FunnelStep("got_order", "Получили заказ", rs.getLong("got_order")),
                        new FunnelStep("proposal", "Сгенерировали отклик", rs.getLong("proposal"))))
                .single();
    }

    public List<CategoryStat> categories(Period period) {
        Map<Long, long[]> stats = new HashMap<>();
        jdbc.sql("""
                        select category_id id, count(*) v from kwork
                        where creation_date >= (cast(:from as timestamptz) at time zone cast(:tz as text)) and creation_date < (cast(:to as timestamptz) at time zone cast(:tz as text))
                        group by 1""")
                .param("from", period.fromTs()).param("to", period.toTs()).param("tz", timezone)
                .query((rs, i) -> stats.computeIfAbsent(rs.getLong("id"), k -> new long[3])[0] = rs.getLong("v"))
                .list();
        jdbc.sql("""
                        select s.subcategory_id id, count(*) v from user_subcategories s
                        join bot_users u on u.id = s.user_id where u.state = 'ACTIVE' and not u.banned group by 1""")
                .query((rs, i) -> stats.computeIfAbsent(rs.getLong("id"), k -> new long[3])[1] = rs.getLong("v"))
                .list();
        jdbc.sql("""
                        select k.category_id id, count(*) v from proposals p join kwork k on k.kwork_order_id = p.kwork_id
                        where p.created_at >= cast(:from as timestamptz) and p.created_at < cast(:to as timestamptz) group by 1""")
                .param("from", period.fromTs()).param("to", period.toTs())
                .query((rs, i) -> stats.computeIfAbsent(rs.getLong("id"), k -> new long[3])[2] = rs.getLong("v"))
                .list();

        List<CategoryStat> result = new ArrayList<>();
        for (KworkCategoryDto root : catalog.roots()) {
            for (KworkSubcategoryDto sub : root.getCats()) {
                long[] s = stats.getOrDefault(sub.getId(), new long[3]);
                result.add(new CategoryStat(sub.getId(), sub.getName(), root.getName(), s[0], s[1], s[2]));
            }
        }
        result.sort((a, b) -> Long.compare(b.orders() + b.subscribers() * 10, a.orders() + a.subscribers() * 10));
        return result;
    }

    /** Активность пользователей по дню недели (1 — понедельник) и часу. */
    public List<HeatCell> heatmap(Period period) {
        return jdbc.sql("""
                        select extract(isodow from created_at at time zone cast(:tz as text))::int as dow,
                               extract(hour from created_at at time zone cast(:tz as text))::int as hr, count(*) v
                        from bot_events where created_at >= cast(:from as timestamptz) and created_at < cast(:to as timestamptz) group by 1, 2""")
                .param("from", period.fromTs()).param("to", period.toTs()).param("tz", timezone)
                .query((rs, i) -> new HeatCell(rs.getInt("dow"), rs.getInt("hr"), rs.getLong("v")))
                .list();
    }

    public Tokens tokens(Period period) {
        List<ModelTokens> byModel = jdbc.sql("""
                        select coalesce(split_part(model, ':', 1), '—') model, count(*) calls,
                               coalesce(sum(prompt_tokens), 0) p, coalesce(sum(completion_tokens), 0) c
                        from llm_calls where started_at >= cast(:from as timestamptz) and started_at < cast(:to as timestamptz) group by 1 order by 3 + 4 desc""")
                .param("from", period.fromTs()).param("to", period.toTs())
                .query((rs, i) -> new ModelTokens(rs.getString("model"), rs.getLong("calls"), rs.getLong("p"), rs.getLong("c")))
                .list();
        long periodTotal = byModel.stream().mapToLong(m -> m.promptTokens() + m.completionTokens()).sum();
        long allTime = jdbc.sql("select coalesce(sum(coalesce(prompt_tokens, 0) + coalesce(completion_tokens, 0)), 0) from llm_calls")
                .query(Long.class).single();
        return new Tokens(byModel, periodTotal, allTime, settings.tokenBudget());
    }

    public List<Health> health() {
        List<Health> result = new ArrayList<>();
        LocalDateTime nowLocal = LocalDateTime.now(ZoneId.of(timezone));

        // парсер
        Map<String, Object> parser = jdbc.sql("""
                        select max(creation_date) last, count(*) filter (where creation_date >= :hourAgo) last_hour,
                               count(*) filter (where creation_date >= :dayAgo) last_day
                        from kwork""")
                .param("hourAgo", Timestamp.valueOf(nowLocal.minusHours(1)))
                .param("dayAgo", Timestamp.valueOf(nowLocal.minusDays(1)))
                .query((rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Timestamp last = rs.getTimestamp("last");
                    m.put("lastOrderAt", last == null ? null : last.toLocalDateTime());
                    m.put("ordersLastHour", rs.getLong("last_hour"));
                    m.put("ordersLastDay", rs.getLong("last_day"));
                    return m;
                })
                .single();
        LocalDateTime lastOrder = (LocalDateTime) parser.get("lastOrderAt");
        long hoursSilent = lastOrder == null ? Long.MAX_VALUE : Duration.between(lastOrder, nowLocal).toHours();
        result.add(new Health(hoursSilent < 3 ? "ok" : hoursSilent < 12 ? "warn" : "error", "Парсер",
                lastOrder == null ? "Заказов нет" : "Последний заказ " + ago(Duration.between(lastOrder, nowLocal)), parser));

        // рассылка
        Map<String, Object> notifier = jdbc.sql("""
                        select s.last_kwork_id cursor, s.last_run_at last_run,
                               (select count(*) from kwork k where k.kwork_order_id > s.last_kwork_id) lag
                        from notifier_state s where s.id = :id""")
                .param("id", NotifierState.SINGLETON_ID)
                .query((rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cursor", rs.getLong("cursor"));
                    Timestamp lastRun = rs.getTimestamp("last_run");
                    m.put("lastRunAt", lastRun == null ? null : lastRun.toInstant());
                    m.put("lag", rs.getLong("lag"));
                    return m;
                })
                .optional()
                .orElseGet(LinkedHashMap::new);
        notifier.put("enabled", settings.notifierEnabled());
        Instant lastRun = (Instant) notifier.get("lastRunAt");
        boolean stale = lastRun == null || lastRun.isBefore(Instant.now().minus(settings.pollInterval().multipliedBy(3)));
        String notifierStatus = !settings.notifierEnabled() ? "warn" : stale ? "error" : "ok";
        result.add(new Health(notifierStatus, "Рассылка",
                !settings.notifierEnabled() ? "На паузе" : lastRun == null ? "Ещё не запускалась"
                        : "Проход " + ago(Duration.between(lastRun, Instant.now())), notifier));

        // GigaChat
        Map<String, Object> llm = jdbc.sql("""
                        select max(started_at) filter (where status = 'OK') last_ok,
                               max(started_at) filter (where status = 'ERROR') last_error,
                               count(*) filter (where started_at >= now() - interval '1 hour') calls_hour,
                               count(*) filter (where status = 'ERROR' and started_at >= now() - interval '1 hour') errors_hour,
                               (select error from llm_calls where status = 'ERROR' order by started_at desc limit 1) last_error_text
                        from llm_calls""")
                .query((rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Timestamp ok = rs.getTimestamp("last_ok");
                    Timestamp err = rs.getTimestamp("last_error");
                    m.put("lastOkAt", ok == null ? null : ok.toInstant());
                    m.put("lastErrorAt", err == null ? null : err.toInstant());
                    m.put("callsLastHour", rs.getLong("calls_hour"));
                    m.put("errorsLastHour", rs.getLong("errors_hour"));
                    m.put("lastError", rs.getString("last_error_text"));
                    return m;
                })
                .single();
        llm.put("enabled", settings.aiEnabled());
        llm.put("model", settings.aiModel());
        long calls = (long) llm.get("callsLastHour");
        long errors = (long) llm.get("errorsLastHour");
        Instant lastOk = (Instant) llm.get("lastOkAt");
        Instant lastErr = (Instant) llm.get("lastErrorAt");
        String llmStatus = !settings.aiEnabled() ? "warn"
                : calls > 0 && errors * 2 >= calls ? "error"
                : lastErr != null && (lastOk == null || lastErr.isAfter(lastOk)) ? "warn" : "ok";
        result.add(new Health(llmStatus, "GigaChat",
                !settings.aiEnabled() ? "Генерация выключена"
                        : calls == 0 ? "Запросов за час не было" : errors + " ошибок из " + calls + " за час", llm));

        // Telegram
        Map<String, Object> tg = jdbc.sql("""
                        select count(*) filter (where status = 'FAILED' and sent_at >= now() - interval '1 hour') failed_hour,
                               count(*) filter (where status = 'OK' and sent_at >= now() - interval '1 hour') ok_hour,
                               count(*) filter (where status = 'BLOCKED' and sent_at >= now() - interval '1 day') blocked_day
                        from deliveries""")
                .query((rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("failedLastHour", rs.getLong("failed_hour"));
                    m.put("sentLastHour", rs.getLong("ok_hour"));
                    m.put("blockedLastDay", rs.getLong("blocked_day"));
                    return m;
                })
                .single();
        long failed = (long) tg.get("failedLastHour");
        long sent = (long) tg.get("sentLastHour");
        result.add(new Health(failed == 0 ? "ok" : failed > sent ? "error" : "warn", "Telegram",
                failed == 0 ? "Отправлено за час: " + sent : "Ошибок отправки за час: " + failed, tg));

        result.add(new Health(settings.maintenance() ? "warn" : "ok", "Бот",
                settings.maintenance() ? "Включены технические работы" : "Работает", Map.of("maintenance", settings.maintenance())));
        return result;
    }

    static String ago(Duration d) {
        if (d.isNegative() || d.toMinutes() < 1) return "только что";
        if (d.toHours() < 1) return d.toMinutes() + " мин назад";
        if (d.toDays() < 1) return d.toHours() + " ч назад";
        return d.toDays() + " дн назад";
    }
}
