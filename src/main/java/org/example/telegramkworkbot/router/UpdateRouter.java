package org.example.telegramkworkbot.router;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.admin.AdminProperties;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.settings.SettingsService;
import org.example.telegramkworkbot.tracking.ActivityTracker;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class UpdateRouter extends DefaultLongPollingUpdateConsumer {

    private final CallbackRouter callbackRouter;
    private final CommandRouter commandRouter;
    private final TextRouter textRouter;
    private final ActivityTracker activity;
    private final SettingsService settings;
    private final AdminProperties admin;
    private final MessageSender sender;

    @Override
    public void consume(Update update) {
        User from = update.hasCallbackQuery() ? update.getCallbackQuery().getFrom()
                : update.hasMessage() ? update.getMessage().getFrom() : null;
        if (from == null || activity.isBanned(from.getId())) {
            return; // забаненным не отвечаем совсем
        }
        activity.touch(from.getId());

        if (settings.maintenance() && !admin.isAdmin(from.getId())) {
            replyMaintenance(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            callbackRouter.route(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            if (CommandRouter.extractCommand(message.getText()) != null) {
                commandRouter.route(message);
            } else {
                textRouter.route(message);
            }
        }
    }

    private void replyMaintenance(Update update) {
        if (update.hasCallbackQuery()) {
            var query = update.getCallbackQuery();
            sender.send(new CallbackAnswer(query.getMessage().getChatId(), query.getId(), settings.maintenanceText()));
        } else if (update.hasMessage()) {
            sender.send(new TextMessage(update.getMessage().getChatId(), settings.maintenanceText()));
        }
    }
}
