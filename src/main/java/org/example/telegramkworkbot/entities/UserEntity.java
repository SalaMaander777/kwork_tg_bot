package org.example.telegramkworkbot.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.Setter;
import org.example.telegramkworkbot.enums.UserState;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bot_users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    private Long id; // id пользователя в Telegram, свой генератор не нужен

    private String username;
    private String firstName;
    private String lastName;
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserState state = UserState.NEW;

    private Instant lastActivityAt;

    @Column(nullable = false)
    private boolean banned;

    // персональный дневной лимит откликов; null — общий из настроек
    private Integer dailyLimitOverride;

    // id подкатегорий из kwork_categories.json; с ними сравнивается kwork.category_id
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_subcategories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "subcategory_id")
    private Set<Long> subcategoryIds = new HashSet<>();
}
