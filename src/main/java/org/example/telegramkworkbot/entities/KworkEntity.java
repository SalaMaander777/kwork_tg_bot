package org.example.telegramkworkbot.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name="kwork")
public class KworkEntity {
    @Id
    @Column(name = "kwork_order_id")
    private Long id;
    @Column(name = "kwork_name")
    private String name;
    @Column(name = "kwork_description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "kwork_price_limit")
    private BigDecimal priceLimit;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "max_days")
    private Integer maxDays;
    @Column(name = "creation_date")
    private LocalDateTime dateCreate;
    @Column(name = "username")
    private String username;
    @Column(name = "user_profile")
    private String userProfileUrl;
    @Column(name = "kwork_count")
    private int kworkCount;
    // Ведёт парсер: false — заказ пропал с сайта. Бот эти поля только читает.
    @Column(name = "first_seen_at", insertable = false, updatable = false)
    private LocalDateTime firstSeenAt;
    @Column(name = "is_active", insertable = false, updatable = false)
    private boolean active = true;

    public KworkEntity(){

    }

    @Override
    public String toString() {
        return "KworkEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", priceLimit=" + priceLimit +
                ", categoryId=" + categoryId +
                ", maxDays=" + maxDays +
                ", dateCreate=" + dateCreate +
                ", username='" + username + '\'' +
                ", userProfileUrl='" + userProfileUrl + '\'' +
                ", kworkCount=" + kworkCount +
                '}';
    }
}

