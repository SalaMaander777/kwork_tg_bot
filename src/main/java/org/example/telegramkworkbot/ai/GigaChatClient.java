package org.example.telegramkworkbot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Клиент GigaChat API: OAuth-токен (живёт 30 минут, кэшируется) + chat/completions.
 * Число одновременных запросов ограничено (у физлиц — один поток), остальные ждут в очереди.
 * https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/gigachat-api
 */
@Slf4j
public class GigaChatClient implements LlmClient {

    private static final MediaType JSON = MediaType.get("application/json");
    private static final long TOKEN_REFRESH_MARGIN_MS = 60_000;

    private final GigaChatProperties props;
    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final Semaphore slots;

    private String accessToken;
    private long tokenExpiresAt;

    public GigaChatClient(GigaChatProperties props, OkHttpClient http, ObjectMapper mapper) {
        this.props = props;
        this.http = http;
        this.mapper = mapper;
        this.slots = new Semaphore(Math.max(1, props.maxConcurrent()), true);
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        if (props.basicCredentials() == null) {
            throw new LlmException("Не заданы ключи GigaChat: GIGACHAT_AUTH_KEY или GIGACHAT_CLIENT_ID + GIGACHAT_CLIENT_SECRET");
        }
        acquireSlot();
        try {
            return chat(messages, token(false));
        } catch (UnauthorizedException e) {
            // токен могли отозвать раньше срока — получаем новый и пробуем ещё раз
            return chat(messages, token(true));
        } finally {
            slots.release();
        }
    }

    private void acquireSlot() {
        try {
            // ждём не дольше таймаута запроса, иначе пользователь так и не получит ответ
            if (!slots.tryAcquire(props.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                throw new LlmException("GigaChat занят другими запросами");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Ожидание GigaChat прервано", e);
        }
    }

    private String chat(List<ChatMessage> messages, String token) {
        ObjectNode body = mapper.createObjectNode()
                .put("model", props.model())
                .put("temperature", props.temperature());
        ArrayNode items = body.putArray("messages");
        for (ChatMessage m : messages) {
            items.addObject()
                    .put("role", m.role().name().toLowerCase(Locale.ROOT))
                    .put("content", m.content());
        }

        Request request = new Request.Builder()
                .url(props.apiUrl())
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        JsonNode response = execute(request);
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new LlmException("GigaChat вернул пустой ответ");
        }
        return content.strip();
    }

    private synchronized String token(boolean forceRefresh) {
        if (!forceRefresh && accessToken != null && System.currentTimeMillis() < tokenExpiresAt - TOKEN_REFRESH_MARGIN_MS) {
            return accessToken;
        }
        Request request = new Request.Builder()
                .url(props.oauthUrl())
                .header("Authorization", "Basic " + props.basicCredentials())
                .header("RqUID", UUID.randomUUID().toString())
                .header("Accept", "application/json")
                .post(new FormBody.Builder().add("scope", props.scope()).build())
                .build();

        JsonNode response;
        try {
            response = execute(request);
        } catch (UnauthorizedException e) {
            throw new LlmException("GigaChat отклонил ключ авторизации");
        }
        accessToken = response.path("access_token").asText(null);
        tokenExpiresAt = response.path("expires_at").asLong(0);
        if (accessToken == null) {
            throw new LlmException("GigaChat не вернул access_token");
        }
        log.debug("Получен токен GigaChat, действует до {}", tokenExpiresAt);
        return accessToken;
    }

    private JsonNode execute(Request request) {
        try (Response response = http.newCall(request).execute()) {
            ResponseBody body = response.body();
            String text = body == null ? "" : body.string();
            if (response.code() == 401) {
                throw new UnauthorizedException();
            }
            if (response.code() == 429) {
                throw new LlmException("GigaChat: превышен лимит запросов или закончились токены");
            }
            if (!response.isSuccessful()) {
                throw new LlmException("GigaChat HTTP " + response.code() + ": " + text);
            }
            return mapper.readTree(text);
        } catch (IOException e) {
            throw new LlmException("Ошибка запроса к GigaChat: " + e.getMessage(), e);
        }
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}
