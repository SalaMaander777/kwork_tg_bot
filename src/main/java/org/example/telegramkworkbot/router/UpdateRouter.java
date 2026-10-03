package org.example.telegramkworkbot.router;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class UpdateRouter extends DefaultLongPollingUpdateConsumer {

    private final CallbackRouter callbackRouter;
    private final CommandRouter commandRouter;

    @Override
    public void consume(Update update) {

        if (update.hasCallbackQuery()) {
            callbackRouter.route(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            commandRouter.route(update.getMessage());
        }
    }
}
