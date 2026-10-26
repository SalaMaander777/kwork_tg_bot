package org.example.telegramkworkbot.admin.api;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.repository.KworkRepo;
import org.example.telegramkworkbot.service.CategoryCatalog;
import org.example.telegramkworkbot.service.KworkCardFormatter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Заказы из таблицы парсера с данными о доставках и откликах. */
@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    public record OrderRow(long id, String name, BigDecimal price, Integer maxDays, Long categoryId, String category,
                           LocalDateTime createdAt, String buyer, int responses, long deliveries, long proposals) {}

    public record Recipient(long userId, String username, String status, Instant sentAt) {}

    public record OrderDetails(OrderRow order, String description, String url, String cardHtml,
                               List<Recipient> recipients, List<AdminProposalService.ProposalRow> proposals) {}

    private final JdbcClient jdbc;
    private final KworkRepo kworkRepo;
    private final CategoryCatalog catalog;
    private final KworkCardFormatter cardFormatter;
    private final AdminProposalService proposals;

    @GetMapping
    public PageResult<OrderRow> list(@RequestParam(required = false) String query,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) BigDecimal priceMin,
                                     @RequestParam(required = false) Boolean delivered,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "25") int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (query != null && !query.isBlank()) {
            where.append(" and (k.kwork_name ilike :q or k.kwork_description ilike :q or cast(k.kwork_order_id as text) = :exact)");
            params.put("q", "%" + query.strip().replace("%", "\\%").replace("_", "\\_") + "%");
            params.put("exact", query.strip());
        }
        if (categoryId != null) {
            where.append(" and k.category_id = :cat");
            params.put("cat", categoryId);
        }
        if (priceMin != null) {
            where.append(" and k.kwork_price_limit >= :price");
            params.put("price", priceMin);
        }
        if (Boolean.TRUE.equals(delivered)) {
            where.append(" and exists (select 1 from deliveries d where d.kwork_id = k.kwork_order_id)");
        }
        int pageSize = UsersController.clamp(size);
        long total = jdbc.sql("select count(*) from kwork k" + where).params(params).query(Long.class).single();
        List<OrderRow> items = jdbc.sql(ORDER_SELECT + where + " order by k.kwork_order_id desc limit :limit offset :offset")
                .params(params).param("limit", pageSize).param("offset", Math.max(page, 0) * pageSize)
                .query((rs, i) -> orderRow(rs))
                .list();
        return new PageResult<>(items, total, page, pageSize);
    }

    @GetMapping("/{id}")
    public OrderDetails details(@PathVariable long id) {
        KworkEntity kwork = kworkRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Заказ не найден"));
        OrderRow row = jdbc.sql(ORDER_SELECT + " where k.kwork_order_id = :id").param("id", id).query((rs, i) -> orderRow(rs)).single();
        List<Recipient> recipients = jdbc.sql("""
                        select d.user_id, u.username, d.status, d.sent_at from deliveries d
                        left join bot_users u on u.id = d.user_id where d.kwork_id = :id order by d.id""")
                .param("id", id)
                .query((rs, i) -> new Recipient(rs.getLong("user_id"), rs.getString("username"), rs.getString("status"),
                        AdminUserService.instant(rs, "sent_at")))
                .list();
        return new OrderDetails(row, kwork.getDescription(), KworkCardFormatter.url(id), cardFormatter.text(kwork),
                recipients, proposals.list(null, id, 0, 50).items());
    }

    private static final String ORDER_SELECT = """
            select k.kwork_order_id, k.kwork_name, k.kwork_price_limit, k.max_days, k.category_id, k.creation_date,
                   k.username, k.kwork_count,
                   (select count(*) from deliveries d where d.kwork_id = k.kwork_order_id and d.status = 'OK') deliveries,
                   (select count(*) from proposals p where p.kwork_id = k.kwork_order_id) proposals
            from kwork k""";

    private OrderRow orderRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long categoryId = (Long) rs.getObject("category_id");
        var created = rs.getTimestamp("creation_date");
        return new OrderRow(rs.getLong("kwork_order_id"), rs.getString("kwork_name"), rs.getBigDecimal("kwork_price_limit"),
                (Integer) rs.getObject("max_days"), categoryId,
                categoryId == null ? null : catalog.sub(categoryId).map(s -> s.getName()).orElse(null),
                created == null ? null : created.toLocalDateTime(), rs.getString("username"), rs.getInt("kwork_count"),
                rs.getLong("deliveries"), rs.getLong("proposals"));
    }
}
