package org.example.telegramkworkbot.router;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRouterTest {

    @Test
    void extractsCommand() {
        assertThat(CommandRouter.extractCommand("/start")).isEqualTo("/start");
        assertThat(CommandRouter.extractCommand("/start payload")).isEqualTo("/start");
        assertThat(CommandRouter.extractCommand("/Profile@my_bot")).isEqualTo("/profile");
    }

    @Test
    void plainTextIsNotCommand() {
        assertThat(CommandRouter.extractCommand("Я Java-разработчик")).isNull();
        assertThat(CommandRouter.extractCommand(null)).isNull();
    }
}
