package org.example.telegramkworkbot.settings;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.service.PromptBuilder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Настройки бота, которые можно менять из админки без перезапуска.
 * Значение берётся из таблицы app_settings, иначе — значение по умолчанию (из application.yaml).
 */
@Slf4j
@Service
public class SettingsService {

    public static final String NOTIFIER_ENABLED = "notifier.enabled";
    public static final String NOTIFIER_POLL_SECONDS = "notifier.poll-interval-seconds";
    public static final String NOTIFIER_BATCH_SIZE = "notifier.batch-size";
    public static final String AI_ENABLED = "ai.enabled";
    public static final String AI_MODEL = "ai.model";
    public static final String AI_TEMPERATURE = "ai.temperature";
    public static final String AI_DAILY_LIMIT = "ai.daily-limit";
    public static final String AI_SYSTEM_PROMPT = "ai.system-prompt";
    public static final String AI_TOKEN_BUDGET = "ai.token-budget";
    public static final String BOT_MAINTENANCE = "bot.maintenance";
    public static final String BOT_MAINTENANCE_TEXT = "bot.maintenance-text";

    public static final List<String> MODELS = List.of("GigaChat", "GigaChat-2", "GigaChat-2-Pro", "GigaChat-2-Max", "GigaChat-3-Ultra");

    private final JdbcClient jdbc;
    private final Map<String, SettingDef> defs = new LinkedHashMap<>();
    private final Map<String, String> values = new ConcurrentHashMap<>();

    public SettingsService(JdbcClient jdbc, Environment env) {
        this.jdbc = jdbc;
        long pollSeconds = env.getProperty("kwork.poll-interval", Duration.class, Duration.ofMinutes(1)).toSeconds();

        def(NOTIFIER_ENABLED, SettingDef.Type.BOOLEAN, "Рассылка", "Рассылка заказов",
                "Если выключить, новые заказы не рассылаются (и не копятся до включения)", "true", null, null);
        def(NOTIFIER_POLL_SECONDS, SettingDef.Type.INTEGER, "Рассылка", "Интервал проверки, сек",
                "Как часто проверять новые заказы в базе", String.valueOf(pollSeconds), 10.0, 3600.0);
        def(NOTIFIER_BATCH_SIZE, SettingDef.Type.INTEGER, "Рассылка", "Заказов за проход",
                "Сколько новых заказов обрабатывать за один проход", env.getProperty("kwork.batch-size", "100"), 1.0, 1000.0);
        def(AI_ENABLED, SettingDef.Type.BOOLEAN, "AI", "Генерация откликов",
                "Если выключить, кнопка «Отклик» сообщает, что AI временно недоступен", "true", null, null);
        defs.put(AI_MODEL, new SettingDef(AI_MODEL, SettingDef.Type.CHOICE, "AI", "Модель",
                "Модель GigaChat для откликов", env.getProperty("ai.gigachat.model", "GigaChat-2-Max"), null, null, MODELS));
        def(AI_TEMPERATURE, SettingDef.Type.DECIMAL, "AI", "Температура",
                "Чем выше, тем разнообразнее и смелее тексты", env.getProperty("ai.gigachat.temperature", "0.8"), 0.0, 2.0);
        def(AI_DAILY_LIMIT, SettingDef.Type.INTEGER, "AI", "Лимит откликов в день",
                "Для каждого пользователя; можно переопределить в карточке пользователя", env.getProperty("ai.daily-limit", "20"), 0.0, 1000.0);
        def(AI_SYSTEM_PROMPT, SettingDef.Type.TEXT, "AI", "Системный промпт",
                "Инструкция модели: как писать отклик", PromptBuilder.SYSTEM, null, 8000.0);
        def(AI_TOKEN_BUDGET, SettingDef.Type.INTEGER, "AI", "Бюджет токенов",
                "Сколько токенов доступно в пакете (для прогноза остатка на дашборде); 0 — не показывать", "0", 0.0, 1e12);
        def(BOT_MAINTENANCE, SettingDef.Type.BOOLEAN, "Бот", "Технические работы",
                "Бот отвечает всем, кроме администратора, текстом ниже", "false", null, null);
        def(BOT_MAINTENANCE_TEXT, SettingDef.Type.TEXT, "Бот", "Текст технических работ",
                null, "🛠 Ведутся технические работы, скоро вернёмся!", null, 1000.0);

        reload();
    }

    private void def(String key, SettingDef.Type type, String group, String label, String description,
                     String defaultValue, Double min, Double max) {
        defs.put(key, new SettingDef(key, type, group, label, description, defaultValue, min, max, List.of()));
    }

    public void reload() {
        values.clear();
        jdbc.sql("select key, value from app_settings")
                .query((rs, i) -> Map.entry(rs.getString(1), rs.getString(2)))
                .list()
                .forEach(e -> {
                    if (defs.containsKey(e.getKey())) values.put(e.getKey(), e.getValue());
                });
    }

    public record SettingView(SettingDef def, String value, boolean overridden) {}

    public List<SettingView> list() {
        List<SettingView> result = new ArrayList<>();
        defs.values().forEach(def -> result.add(new SettingView(def, get(def.key()), values.containsKey(def.key()))));
        return result;
    }

    /**
     * Проверяет и сохраняет значения. Значение, равное значению по умолчанию, удаляет переопределение.
     *
     * @return ошибки по ключам; если не пусто — ничего не сохранено
     */
    public Map<String, String> update(Map<String, String> changes) {
        Map<String, String> errors = new LinkedHashMap<>();
        changes.forEach((key, value) -> {
            SettingDef def = defs.get(key);
            String error = def == null ? "Неизвестная настройка" : def.validate(value);
            if (error != null) errors.put(key, error);
        });
        if (!errors.isEmpty()) {
            return errors;
        }
        Timestamp now = Timestamp.from(Instant.now());
        changes.forEach((key, value) -> {
            String normalized = defs.get(key).type() == SettingDef.Type.TEXT ? value : value.strip();
            if (normalized.equals(defs.get(key).defaultValue())) {
                jdbc.sql("delete from app_settings where key = ?").param(key).update();
            } else {
                jdbc.sql("""
                                insert into app_settings (key, value, updated_at) values (?, ?, ?)
                                on conflict (key) do update set value = excluded.value, updated_at = excluded.updated_at""")
                        .params(key, normalized, now)
                        .update();
            }
        });
        reload();
        log.info("Изменены настройки: {}", changes.keySet());
        return errors;
    }

    public String get(String key) {
        String value = values.get(key);
        return value != null ? value : defs.get(key).defaultValue();
    }

    public boolean notifierEnabled() {
        return Boolean.parseBoolean(get(NOTIFIER_ENABLED));
    }

    public Duration pollInterval() {
        return Duration.ofSeconds(Long.parseLong(get(NOTIFIER_POLL_SECONDS)));
    }

    public int batchSize() {
        return Integer.parseInt(get(NOTIFIER_BATCH_SIZE));
    }

    public boolean aiEnabled() {
        return Boolean.parseBoolean(get(AI_ENABLED));
    }

    public String aiModel() {
        return get(AI_MODEL);
    }

    public double aiTemperature() {
        return Double.parseDouble(get(AI_TEMPERATURE));
    }

    public int dailyLimit() {
        return Integer.parseInt(get(AI_DAILY_LIMIT));
    }

    public String systemPrompt() {
        return get(AI_SYSTEM_PROMPT);
    }

    public long tokenBudget() {
        return Long.parseLong(get(AI_TOKEN_BUDGET));
    }

    public boolean maintenance() {
        return Boolean.parseBoolean(get(BOT_MAINTENANCE));
    }

    public String maintenanceText() {
        return get(BOT_MAINTENANCE_TEXT);
    }
}
