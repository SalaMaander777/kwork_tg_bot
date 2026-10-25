package org.example.telegramkworkbot.callback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackDataTest {

    @Test
    void parsesNestedPayload() {
        CallbackData data = CallbackData.parse("category:toggle:44");
        assertThat(data).isEqualTo(new CallbackData("category", "toggle:44"));
        assertThat(CallbackData.parse(data.payload())).isEqualTo(new CallbackData("toggle", "44"));
    }

    @Test
    void actionWithoutPayload() {
        assertThat(CallbackData.parse("done")).isEqualTo(new CallbackData("done", ""));
    }

    @Test
    void encodeRoundTrip() {
        CallbackData data = new CallbackData("reply", "gen:3267577");
        assertThat(CallbackData.parse(data.encode())).isEqualTo(data);
    }
}
