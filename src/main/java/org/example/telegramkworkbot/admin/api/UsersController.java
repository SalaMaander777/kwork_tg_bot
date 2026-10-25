package org.example.telegramkworkbot.admin.api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.sender.SendStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/users")
@RequiredArgsConstructor
public class UsersController {

    public record LimitRequest(Integer limit) {}

    public record StateRequest(String state) {}

    public record MessageRequest(String text) {}

    private final AdminUserService users;
    private final AdminProposalService proposals;

    @GetMapping
    public PageResult<AdminUserService.UserRow> list(@RequestParam(required = false) String query,
                                                     @RequestParam(required = false) String state,
                                                     @RequestParam(required = false) Boolean banned,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "25") int size) {
        return users.list(query, state, banned, sort, Math.max(page, 0), clamp(size));
    }

    @GetMapping("/{id}")
    public AdminUserService.UserDetails details(@PathVariable long id) {
        return users.details(id);
    }

    @GetMapping("/{id}/deliveries")
    public PageResult<AdminUserService.DeliveryRow> deliveries(@PathVariable long id,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "25") int size) {
        return users.deliveries(id, Math.max(page, 0), clamp(size));
    }

    @GetMapping("/{id}/proposals")
    public PageResult<AdminProposalService.ProposalRow> proposals(@PathVariable long id,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "25") int size) {
        return proposals.list(id, null, Math.max(page, 0), clamp(size));
    }

    @PostMapping("/{id}/ban")
    public void ban(@PathVariable long id) {
        users.setBanned(id, true);
    }

    @PostMapping("/{id}/unban")
    public void unban(@PathVariable long id) {
        users.setBanned(id, false);
    }

    @PutMapping("/{id}/limit")
    public void limit(@PathVariable long id, @RequestBody LimitRequest body) {
        users.setDailyLimit(id, body.limit());
    }

    @PutMapping("/{id}/categories")
    public void categories(@PathVariable long id, @RequestBody List<Long> subcategoryIds) {
        users.setSubcategories(id, subcategoryIds);
    }

    @PutMapping("/{id}/state")
    public void state(@PathVariable long id, @RequestBody StateRequest body) {
        users.setState(id, body.state());
    }

    @PostMapping("/{id}/message")
    public Map<String, SendStatus> message(@PathVariable long id, @RequestBody MessageRequest body) {
        return Map.of("status", users.sendMessage(id, body.text()));
    }

    @GetMapping("/export.csv")
    public void export(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"users.csv\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter out = response.getWriter();
        out.print('﻿'); // BOM, чтобы Excel понял UTF-8
        out.println("id;username;first_name;last_name;state;banned;created_at;last_activity_at;subcategories;deliveries;proposals");
        for (AdminUserService.UserRow u : users.all()) {
            out.println(AdminUserService.csvRow(u).stream().map(UsersController::csv).collect(Collectors.joining(";")));
        }
    }

    private static String csv(String value) {
        if (value == null || value.equals("null")) return "";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value; // защита от формул в Excel
        return value.contains(";") || value.contains("\"") || value.contains("\n")
                ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    static int clamp(int size) {
        return Math.min(Math.max(size, 1), 200);
    }
}
