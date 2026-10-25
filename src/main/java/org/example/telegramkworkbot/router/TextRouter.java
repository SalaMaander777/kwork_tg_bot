package org.example.telegramkworkbot.router;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.service.UserService;
import org.example.telegramkworkbot.text.TextStateHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextRouter {

    private final Map<UserState, TextStateHandler> handlers;
    private final UserService userService;
    private final MessageSender sender;

    public TextRouter(List<TextStateHandler> handlers, UserService userService, MessageSender sender) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(TextStateHandler::state, Function.identity()));
        this.userService = userService;
        this.sender = sender;
    }

    public void route(Message message) {
        Optional<UserState> state = userService.findState(message.getFrom().getId());
        if (state.isEmpty()) {
            sender.send(new TextMessage(message.getChatId(), "Нажмите /start, чтобы начать"));
            return;
        }

        TextStateHandler handler = handlers.get(state.get());
        if (handler == null) {
            sender.send(new TextMessage(message.getChatId(),
                    "Я понимаю команды: /categories, /profile, /help"));
            return;
        }
        handler.handle(message);
    }
}
