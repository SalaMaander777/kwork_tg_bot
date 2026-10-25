package org.example.telegramkworkbot.callback;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.sender.MessageSender;
import org.example.telegramkworkbot.service.ProposalService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/** reply:gen:&lt;kworkId&gt; — сгенерировать (ещё один) отклик на заказ. */
@Component
@RequiredArgsConstructor
public class ProposalCallbackHandler implements CallbackHandler {

    private final ProposalService proposalService;
    private final MessageSender sender;

    @Override
    public String action() {
        return "reply";
    }

    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long chatId = query.getMessage().getChatId();
        CallbackData op = CallbackData.parse(data.payload());
        if (!"gen".equals(op.action())) {
            sender.send(new CallbackAnswer(chatId, query.getId(), null));
            return;
        }
        sender.send(new CallbackAnswer(chatId, query.getId(), "Генерирую отклик…"));
        proposalService.generate(chatId, query.getFrom().getId(), Long.parseLong(op.payload()));
    }
}
