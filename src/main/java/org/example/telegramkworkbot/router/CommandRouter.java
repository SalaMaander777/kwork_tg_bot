package org.example.telegramkworkbot.router;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.commands.CommandHandler;
import org.example.telegramkworkbot.tracking.EventRecorder;
import org.example.telegramkworkbot.tracking.EventType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommandRouter {

    private final Map<String, CommandHandler> handlers;
    private final EventRecorder events;

    public CommandRouter(List<CommandHandler> handlers, EventRecorder events) {
        this.events = events;
        // toMap бросит исключение при старте, если у двух хендлеров одинаковая команда
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(CommandHandler::command, Function.identity()));
    }

    public void route(Message message) {
        String command = extractCommand(message.getText());
        if (command == null) {
            return; // обычный текст, не команда
        }

        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            log.debug("Unknown command: {}", command);
            return;
        }
        events.record(message.getFrom().getId(), EventType.COMMAND, command);
        handler.handle(message);
    }

    /**
     * "/start" -> "/start", "/start payload" -> "/start",
     * "/start@my_bot" -> "/start", "hello" -> null
     */
    static String extractCommand(String text) {
        if (text == null || !text.startsWith("/")) {
            return null;
        }
        String firstWord = text.split("\\s+", 2)[0];
        int at = firstWord.indexOf('@');
        String command = at < 0 ? firstWord : firstWord.substring(0, at);
        return command.toLowerCase(Locale.ROOT);
    }
}