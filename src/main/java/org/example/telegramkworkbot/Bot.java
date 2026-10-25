package org.example.telegramkworkbot;

import org.example.telegramkworkbot.router.UpdateRouter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;


/** Подключение к Telegram. BOT_ENABLED=false — запуск без бота (например, для разработки админки). */
@Component
@ConditionalOnProperty(name = "bot.enabled", havingValue = "true", matchIfMissing = true)
public class Bot implements SpringLongPollingBot {
    private final UpdateRouter router;
    private final String token;

    public Bot(@Value("${bot.token}") String token, UpdateRouter router){
        this.router = router;
        this.token = token;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return router;
    }
}
