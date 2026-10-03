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



    @ManyToMany
    @JoinTable(
            name = "user_categories",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_user_category",
                            columnNames = {"user_id", "category_id"}
                    )
            }


    )
    private Set<CategoryEntity> categories = new HashSet<>();
}