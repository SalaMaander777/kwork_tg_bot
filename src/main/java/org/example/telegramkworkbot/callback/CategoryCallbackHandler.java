package org.example.telegramkworkbot.callback;


import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.service.CategorySelectionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
@Component
@RequiredArgsConstructor
public class CategoryCallbackHandler implements CallbackHandler{
    private final CategorySelectionService selectionService;
    @Override
    public String action() {
        return "category";
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        selectionService.handle(query, data);

    }
}
