package org.example.telegramkworkbot.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.example.telegramkworkbot.ai.GigaChatClient;
import org.example.telegramkworkbot.ai.GigaChatProperties;
import org.example.telegramkworkbot.ai.LlmClient;
import org.example.telegramkworkbot.ai.TrustedCertificates;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(GigaChatProperties.class)
public class AiConfig {

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gigachat", matchIfMissing = true)
    public LlmClient gigaChatClient(GigaChatProperties props, ObjectMapper objectMapper) {
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .callTimeout(props.timeout())
                .readTimeout(props.timeout());

        TrustedCertificates.withExtra(props.caCert()).ifPresentOrElse(
                tls -> http.sslSocketFactory(tls.socketFactory(), tls.trustManager()),
                () -> log.warn("Сертификат НУЦ Минцифры не найден ({}), запросы к GigaChat, скорее всего, "
                        + "упадут с ошибкой TLS. Сертификаты: https://developers.sber.ru/docs/ru/gigachat/certificates", props.caCert()));

        return new GigaChatClient(props, http.build(), objectMapper);
    }
}
