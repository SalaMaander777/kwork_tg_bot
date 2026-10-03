package org.example.telegramkworkbot.router;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.commands.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommandRouter {

    private final List<CommandHandler> handlers;


    public void route(Message message) {
        String text = message.getText();

        for (CommandHandler handler: handlers){
            if (handler.command().equals(text)){
                handler.handle(message);
                return;
            }
        }

        log.debug("Unknown command");
    }


}
