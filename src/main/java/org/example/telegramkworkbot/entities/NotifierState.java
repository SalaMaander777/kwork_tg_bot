package org.example.telegramkworkbot.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifier_state")
@Getter
@Setter
@NoArgsConstructor
public class NotifierState {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false)
    private Long lastKworkId;

    /** Курсор рассылки — пара (время появления, id) последнего обработанного заказа; id разрешает равные времена. */
    private Instant lastActivatedAt;

    private Instant lastRunAt;

    public NotifierState(Long lastKworkId, Instant lastActivatedAt) {
        this.id = SINGLETON_ID;
        this.lastKworkId = lastKworkId;
        this.lastActivatedAt = lastActivatedAt;
    }
}
