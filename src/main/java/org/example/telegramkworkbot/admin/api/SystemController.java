package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.admin.AdminProperties;
import org.example.telegramkworkbot.admin.AuditService;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.service.CategoryCatalog;
import org.example.telegramkworkbot.service.KworkNotifier;
import org.example.telegramkworkbot.settings.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Настройки, управление рассылкой заказов, журнал действий, информация о системе. */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class SystemController {

    public record AuditRow(long id, String action, String target, String details, String ip, Instant createdAt) {}

    public record Category(long id, String name, List<Category> children) {}

    private static final List<String> TABLES = List.of(
            "kwork", "bot_users", "user_subcategories", "freelancer_profiles", "proposals",
            "deliveries", "llm_calls", "bot_events", "broadcasts", "admin_audit");

    private final SettingsService settings;
    private final KworkNotifier notifier;
    private final AuditService audit;
    private final JdbcClient jdbc;
    private final CategoryCatalog catalog;
    private final AdminProperties admin;

    @GetMapping("/settings")
    public List<SettingsService.SettingView> settings() {
        return settings.list();
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> changes) {
        Map<String, String> errors = settings.update(changes);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Проверьте значения", "fields", errors));
        }
        changes.forEach((key, value) -> audit.record("SETTING", key,
                key.equals(SettingsService.AI_SYSTEM_PROMPT) ? value.length() + " символов" : value));
        return ResponseEntity.ok(settings.list());
    }

    @PostMapping("/notifier/run-now")
    public Map<String, Integer> runNow() {
        audit.record("NOTIFIER_RUN", null, null);
        return Map.of("processed", notifier.poll());
    }

    @PostMapping("/notifier/reset-cursor")
    public Map<String, Long> resetCursor() {
        long cursor = notifier.resetCursor();
        audit.record("NOTIFIER_RESET", String.valueOf(cursor), null);
        return Map.of("cursor", cursor);
    }

    @GetMapping("/audit")
    public PageResult<AuditRow> audit(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        int pageSize = UsersController.clamp(size);
        long total = jdbc.sql("select count(*) from admin_audit").query(Long.class).single();
        List<AuditRow> items = jdbc.sql("select * from admin_audit order by id desc limit :limit offset :offset")
                .param("limit", pageSize).param("offset", Math.max(page, 0) * pageSize)
                .query((rs, i) -> new AuditRow(rs.getLong("id"), rs.getString("action"), rs.getString("target"),
                        rs.getString("details"), rs.getString("ip"), AdminUserService.instant(rs, "created_at")))
                .list();
        return new PageResult<>(items, total, page, pageSize);
    }

    /** Справочники для форм: дерево категорий, состояния, модели. */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", catalog.roots().stream()
                .map(root -> new Category(root.getId(), root.getName(), root.getCats().stream()
                        .map(sub -> new Category(sub.getId(), sub.getName(), List.of())).toList()))
                .toList());
        result.put("states", Arrays.stream(UserState.values()).map(Enum::name).toList());
        result.put("models", SettingsService.MODELS);
        result.put("adminTelegramId", admin.telegramId());
        return result;
    }

    @GetMapping("/system")
    public Map<String, Object> system() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> result = new LinkedHashMap<>();
        Package pkg = getClass().getPackage();
        result.put("version", pkg.getImplementationVersion() == null ? "dev" : pkg.getImplementationVersion());
        result.put("java", System.getProperty("java.version"));
        result.put("startedAt", Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()));
        result.put("uptimeSeconds", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toSeconds());
        result.put("memoryUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        result.put("memoryMaxMb", rt.maxMemory() / 1024 / 1024);
        result.put("threads", Thread.activeCount());
        result.put("dbSizeMb", jdbc.sql("select pg_database_size(current_database()) / 1024 / 1024").query(Long.class).single());
        Map<String, Map<String, Long>> tables = new LinkedHashMap<>();
        for (String table : TABLES) {
            tables.put(table, Map.of(
                    "rows", jdbc.sql("select count(*) from " + table).query(Long.class).single(),
                    "sizeKb", jdbc.sql("select pg_total_relation_size(cast(? as regclass)) / 1024").param(table).query(Long.class).single()));
        }
        result.put("tables", tables);
        return result;
    }
}
