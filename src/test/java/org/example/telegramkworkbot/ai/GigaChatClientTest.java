package org.example.telegramkworkbot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GigaChatClientTest {

    private static final List<ChatMessage> PROMPT = List.of(ChatMessage.system("sys"), ChatMessage.user("hi"));

    private MockWebServer server;
    private GigaChatClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = client("secret-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void obtainsTokenOnceAndReusesIt() throws InterruptedException {
        server.enqueue(token("t1"));
        server.enqueue(answer("Первый"));
        server.enqueue(answer("Второй"));

        assertThat(client.complete(PROMPT)).isEqualTo("Первый");
        assertThat(client.complete(PROMPT)).isEqualTo("Второй");

        RecordedRequest oauth = server.takeRequest();
        assertThat(oauth.getPath()).isEqualTo("/oauth");
        assertThat(oauth.getHeader("Authorization")).isEqualTo("Basic secret-key");
        assertThat(oauth.getHeader("RqUID")).isNotBlank();
        assertThat(oauth.getBody().readUtf8()).isEqualTo("scope=GIGACHAT_API_PERS");

        RecordedRequest chat = server.takeRequest();
        assertThat(chat.getPath()).isEqualTo("/chat");
        assertThat(chat.getHeader("Authorization")).isEqualTo("Bearer t1");
        assertThat(chat.getBody().readUtf8())
                .contains("\"model\":\"GigaChat\"")
                .contains("{\"role\":\"system\",\"content\":\"sys\"}");

        assertThat(server.takeRequest().getPath()).isEqualTo("/chat");
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void refreshesTokenOn401() throws InterruptedException {
        server.enqueue(token("old"));
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(token("new"));
        server.enqueue(answer("Ок"));

        assertThat(client.complete(PROMPT)).isEqualTo("Ок");

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer new");
    }

    @Test
    void serverErrorBecomesLlmException() {
        server.enqueue(token("t"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        assertThatThrownBy(() -> client.complete(PROMPT))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("500");
    }

    @Test
    void releasesSlotAfterFailure() {
        server.enqueue(token("t"));
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(answer("После ошибки"));

        assertThatThrownBy(() -> client.complete(PROMPT)).isInstanceOf(LlmException.class);
        // единственный слот освободился — следующий запрос проходит
        assertThat(client.complete(PROMPT)).isEqualTo("После ошибки");
    }

    @Test
    void missingKeyFailsWithoutRequests() {
        assertThatThrownBy(() -> client(" ").complete(PROMPT)).isInstanceOf(LlmException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void buildsKeyFromClientIdAndSecret() {
        GigaChatProperties props = new GigaChatProperties(null, "id", "secret", "GIGACHAT_API_PERS", "GigaChat", 0.8,
                Duration.ofSeconds(5), 1, null, "", "");
        assertThat(props.basicCredentials()).isEqualTo("aWQ6c2VjcmV0"); // base64("id:secret")
    }

    private GigaChatClient client(String authKey) {
        GigaChatProperties props = new GigaChatProperties(authKey, null, null, "GIGACHAT_API_PERS", "GigaChat", 0.8,
                Duration.ofSeconds(5), 1, null, server.url("/oauth").toString(), server.url("/chat").toString());
        return new GigaChatClient(props, new OkHttpClient(), new ObjectMapper());
    }

    private static MockResponse token(String token) {
        long expiresAt = System.currentTimeMillis() + 30 * 60 * 1000;
        return new MockResponse().setBody("{\"access_token\":\"" + token + "\",\"expires_at\":" + expiresAt + "}");
    }

    private static MockResponse answer(String content) {
        return new MockResponse().setBody(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"}}]}");
    }
}
