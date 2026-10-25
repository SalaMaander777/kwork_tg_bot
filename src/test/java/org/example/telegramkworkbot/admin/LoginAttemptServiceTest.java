package org.example.telegramkworkbot.admin;

import org.example.telegramkworkbot.admin.security.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final LoginAttemptService attempts = new LoginAttemptService();

    @Test
    void blocksAfterFiveFailuresPerIp() {
        for (int i = 0; i < 4; i++) {
            attempts.failed("1.1.1.1");
        }
        assertThat(attempts.blockedFor("1.1.1.1")).isNull();

        attempts.failed("1.1.1.1");
        assertThat(attempts.blockedFor("1.1.1.1")).isPositive();
        assertThat(attempts.blockedFor("2.2.2.2")).isNull();
    }

    @Test
    void successResetsCounter() {
        for (int i = 0; i < 4; i++) {
            attempts.failed("1.1.1.1");
        }
        attempts.succeeded("1.1.1.1");
        attempts.failed("1.1.1.1");
        assertThat(attempts.blockedFor("1.1.1.1")).isNull();
    }
}
