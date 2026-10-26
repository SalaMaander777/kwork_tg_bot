package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.admin.AuditService;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.sender.SendStatus;
import org.example.telegramkworkbot.service.CategoryCatalog;
import org.example.telegramkworkbot.service.ProposalService;
import org.example.telegramkworkbot.tracking.ActivityTracker;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Пользователи бота для админки: списки, карточка, управление. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    public record UserRow(long id, String username, String firstName, String lastName, String state, boolean banned,
                          Instant createdAt, Instant lastActivityAt, Integer dailyLimitOverride,
                          long subcategories, long deliveries, long proposals) {}

    public record Subcategory(long id, String name, String root) {}

    public record Event(long id, String type, Instant createdAt, String payload) {}

    public record UserDetails(UserRow user, String profile, Instant profileUpdatedAt, List<Subcategory> subcategories,
                              int dailyLimit, long proposalsToday, List<Event> events) {}

    public record DeliveryRow(long id, long kworkId, String kworkName, String status, Instant sentAt) {}

    private static final Map<String, String> SORTS = Map.of(
            "created", "u.created_at desc nulls last",
            "activity", "u.last_activity_at desc nulls last",
            "proposals", "proposals desc",
            "deliveries", "deliveries desc");

    private static final String USER_SELECT = """
            select u.id, u.username, u.first_name, u.last_name, u.state, u.banned, u.created_at, u.last_activity_at,
                   u.daily_limit_override,
                   (select count(*) from user_subcategories s where s.user_id = u.id) subcategories,
                   (select count(*) from deliveries d where d.user_id = u.id and d.status = 'OK') deliveries,
                   (select count(*) from proposals p where p.user_id = u.id) proposals
            from bot_users u""";

    private final JdbcClient jdbc;
    private final CategoryCatalog catalog;
    private final ActivityTracker activity;
    private final ProposalService proposals;
    private final MessageSender sender;
    private final AuditService audit;

    public PageResult<UserRow> list(String query, String state, Boolean banned, String sort, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        var params = new java.util.HashMap<String, Object>();
        if (query != null && !query.isBlank()) {
            where.append(" and (u.username ilike :q or u.first_name ilike :q or u.last_name ilike :q or cast(u.id as text) = :exact)");
            params.put("q", "%" + query.strip().replace("%", "\\%").replace("_", "\\_") + "%");
            params.put("exact", query.strip());
        }
        if (state != null && !state.isBlank()) {
            parseState(state);
            where.append(" and u.state = :state");
            params.put("state", state);
        }
        if (banned != null) {
            where.append(" and u.banned = :banned");
            params.put("banned", banned);
        }
        long total = jdbc.sql("select count(*) from bot_users u" + where).params(params).query(Long.class).single();
        String order = SORTS.getOrDefault(sort == null ? "created" : sort, SORTS.get("created"));
        List<UserRow> items = jdbc.sql(USER_SELECT + where + " order by " + order + ", u.id limit :limit offset :offset")
                .params(params).param("limit", size).param("offset", page * size)
                .query(AdminUserService::userRow).list();
        return new PageResult<>(items, total, page, size);
    }

    public UserDetails details(long id) {
        UserRow user = jdbc.sql(USER_SELECT + " where u.id = :id").param("id", id)
                .query(AdminUserService::userRow).optional()
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));
        var profile = jdbc.sql("select about, updated_at from freelancer_profiles where user_id = :id").param("id", id)
                .query((rs, i) -> Map.entry(Optional.ofNullable(rs.getString(1)), Optional.ofNullable(instant(rs, "updated_at"))))
                .optional();
        List<Subcategory> subs = jdbc.sql("select subcategory_id from user_subcategories where user_id = :id").param("id", id)
                .query(Long.class).list().stream().map(this::subcategory).toList();
        List<Event> events = jdbc.sql("select id, type, created_at, payload from bot_events where user_id = :id order by id desc limit 100")
                .param("id", id)
                .query((rs, i) -> new Event(rs.getLong("id"), rs.getString("type"), instant(rs, "created_at"), rs.getString("payload")))
                .list();
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long today = jdbc.sql("select count(*) from proposals where user_id = :id and created_at >= :from")
                .param("id", id).param("from", Timestamp.from(startOfDay)).query(Long.class).single();
        return new UserDetails(user,
                profile.flatMap(Map.Entry::getKey).orElse(null),
                profile.flatMap(Map.Entry::getValue).orElse(null),
                subs, proposals.dailyLimit(id), today, events);
    }

    public PageResult<DeliveryRow> deliveries(long userId, int page, int size) {
        long total = jdbc.sql("select count(*) from deliveries where user_id = :id").param("id", userId).query(Long.class).single();
        List<DeliveryRow> items = jdbc.sql("""
                        select d.id, d.kwork_id, k.kwork_name, d.status, d.sent_at from deliveries d
                        left join kwork k on k.kwork_order_id = d.kwork_id
                        where d.user_id = :id order by d.id desc limit :limit offset :offset""")
                .param("id", userId).param("limit", size).param("offset", page * size)
                .query((rs, i) -> new DeliveryRow(rs.getLong("id"), rs.getLong("kwork_id"), rs.getString("kwork_name"),
                        rs.getString("status"), instant(rs, "sent_at")))
                .list();
        return new PageResult<>(items, total, page, size);
    }

    @Transactional
    public void setBanned(long id, boolean banned) {
        requireExists(id);
        jdbc.sql("update bot_users set banned = :banned where id = :id").param("banned", banned).param("id", id).update();
        activity.setBanned(id, banned);
        audit.record(banned ? "USER_BAN" : "USER_UNBAN", String.valueOf(id), null);
    }

    public void setDailyLimit(long id, Integer limit) {
        requireExists(id);
        if (limit != null && (limit < 0 || limit > 1000)) {
            throw new IllegalArgumentException("Лимит от 0 до 1000");
        }
        jdbc.sql("update bot_users set daily_limit_override = :limit where id = :id").param("limit", limit).param("id", id).update();
        audit.record("USER_LIMIT", String.valueOf(id), limit == null ? "по умолчанию" : String.valueOf(limit));
    }

    @Transactional
    public void setSubcategories(long id, Collection<Long> subcategoryIds) {
        requireExists(id);
        for (Long subId : subcategoryIds) {
            if (catalog.sub(subId).isEmpty()) {
                throw new IllegalArgumentException("Неизвестная подкатегория: " + subId);
            }
        }
        jdbc.sql("delete from user_subcategories where user_id = :id").param("id", id).update();
        for (Long subId : subcategoryIds) {
            jdbc.sql("insert into user_subcategories (user_id, subcategory_id) values (:id, :sub)")
                    .param("id", id).param("sub", subId).update();
        }
        audit.record("USER_CATEGORIES", String.valueOf(id), subcategoryIds.toString());
    }

    public void setState(long id, String state) {
        requireExists(id);
        parseState(state);
        jdbc.sql("update bot_users set state = :state where id = :id").param("state", state).param("id", id).update();
        audit.record("USER_STATE", String.valueOf(id), state);
    }

    public SendStatus sendMessage(long id, String html) {
        requireExists(id);
        if (html == null || html.isBlank() || html.length() > 4000) {
            throw new IllegalArgumentException("Текст от 1 до 4000 символов");
        }
        SendStatus status = sender.send(new KeyboardMessage(id, html, null));
        if (status == SendStatus.BLOCKED) {
            jdbc.sql("update bot_users set state = 'BLOCKED' where id = :id").param("id", id).update();
        }
        audit.record("USER_MESSAGE", String.valueOf(id), status.name());
        return status;
    }

    public List<UserRow> all() {
        return jdbc.sql(USER_SELECT + " order by u.created_at desc nulls last").query(AdminUserService::userRow).list();
    }

    private Subcategory subcategory(Long id) {
        String root = catalog.roots().stream()
                .filter(r -> catalog.subIds(r).contains(id)).map(r -> r.getName()).findFirst().orElse("—");
        return new Subcategory(id, catalog.sub(id).map(s -> s.getName()).orElse("#" + id), root);
    }

    private static UserState parseState(String state) {
        try {
            return UserState.valueOf(state);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Неизвестное состояние: " + state);
        }
    }

    private void requireExists(long id) {
        if (jdbc.sql("select count(*) from bot_users where id = :id").param("id", id).query(Long.class).single() == 0) {
            throw new NoSuchElementException("Пользователь не найден");
        }
    }

    private static UserRow userRow(ResultSet rs, int i) throws SQLException {
        return new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("state"), rs.getBoolean("banned"), instant(rs, "created_at"), instant(rs, "last_activity_at"),
                (Integer) rs.getObject("daily_limit_override"),
                rs.getLong("subcategories"), rs.getLong("deliveries"), rs.getLong("proposals"));
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    static List<String> csvRow(UserRow u) {
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(u.id()));
        row.add(u.username());
        row.add(u.firstName());
        row.add(u.lastName());
        row.add(u.state());
        row.add(String.valueOf(u.banned()));
        row.add(String.valueOf(u.createdAt()));
        row.add(String.valueOf(u.lastActivityAt()));
        row.add(String.valueOf(u.subcategories()));
        row.add(String.valueOf(u.deliveries()));
        row.add(String.valueOf(u.proposals()));
        return row;
    }
}
