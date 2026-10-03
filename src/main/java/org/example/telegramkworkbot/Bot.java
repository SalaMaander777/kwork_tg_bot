package org.example.telegramkworkbot;

import org.example.telegramkworkbot.router.UpdateRouter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;


@Component
public class Bot implements SpringLongPollingBot {
    private UpdateRouter router;
    @Override
    public String getBotToken() {
        return "";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return router;
    }
}
