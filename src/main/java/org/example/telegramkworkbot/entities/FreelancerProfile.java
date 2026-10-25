package org.example.telegramkworkbot.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "freelancer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class FreelancerProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String about;

    private Instant updatedAt;
}
