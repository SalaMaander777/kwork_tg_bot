package org.example.telegramkworkbot.admin.api;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/** Интервал [from, to) для метрик и предыдущий интервал той же длины — для сравнения. */
public record Period(Instant from, Instant to) {

    public static Period of(Instant from, Instant to, Integer days) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(Duration.ofDays(days != null ? days : 30));
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Начало периода должно быть раньше конца");
        }
        if (Duration.between(start, end).toDays() > 366) {
            throw new IllegalArgumentException("Период не длиннее года");
        }
        return new Period(start, end);
    }

    public Period previous() {
        Duration length = Duration.between(from, to);
        return new Period(from.minus(length), from);
    }

    /** Часовые бакеты для коротких периодов, дневные — для длинных. */
    public String defaultBucket() {
        return Duration.between(from, to).toHours() <= 48 ? "hour" : "day";
    }

    public Timestamp fromTs() {
        return Timestamp.from(from);
    }

    public Timestamp toTs() {
        return Timestamp.from(to);
    }
}
