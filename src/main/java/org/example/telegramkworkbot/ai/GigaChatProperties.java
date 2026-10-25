package org.example.telegramkworkbot.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Авторизация: либо authKey (Authorization key из кабинета developers.sber.ru), либо пара clientId + clientSecret.
 *
 * @param authKey ключ авторизации — Base64 от "client_id:client_secret"
 * @param caCert        сертификаты НУЦ Минцифры (корневой + выпускающий в одном PEM), которыми подписаны серверы GigaChat
 * @param maxConcurrent сколько запросов можно выполнять одновременно; физлицам доступен один поток
 */
@ConfigurationProperties("ai.gigachat")
public record GigaChatProperties(
        String authKey,
        String clientId,
        String clientSecret,
        @DefaultValue("GIGACHAT_API_PERS") String scope,
        @DefaultValue("GigaChat") String model,
        @DefaultValue("0.8") double temperature,
        @DefaultValue("PT60S") Duration timeout,
        @DefaultValue("1") int maxConcurrent,
        Resource caCert,
        @DefaultValue("https://ngw.devices.sberbank.ru:9443/api/v2/oauth") String oauthUrl,
        @DefaultValue("https://api.giga.chat/v1/chat/completions") String apiUrl
) {

    /** Значение для заголовка Authorization: Basic ... ; null, если ключи не заданы. */
    public String basicCredentials() {
        if (authKey != null && !authKey.isBlank()) {
            return authKey.strip();
        }
        if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
            String pair = clientId.strip() + ":" + clientSecret.strip();
            return Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }
}
