package org.example.telegramkworkbot.router;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.callback.CallbackData;
import org.example.telegramkworkbot.callback.CallbackHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackRouter {
    private final List<CallbackHandler> callbackHandlers;
    public void route(CallbackQuery callbackQuery){
        CallbackData data = CallbackData.parse(callbackQuery.getData());


        for (CallbackHandler callbackHandler: callbackHandlers){
            if(callbackHandler.action().equals(data.action())){
                callbackHandler.handle(callbackQuery, data);
                return;
            }
        }

    };
}
