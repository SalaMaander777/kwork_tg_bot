package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.entities.Proposal;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.Html;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Вид «отклик» для сообщения с карточкой заказа: текст в цитате,
 * переключение на карточку, новый вариант и листание вариантов.
 */
@Component
public class ProposalView {

    static final String NOOP = "noop";

    public String text(KworkEntity kwork, Proposal proposal) {
        return "✍️ <b>Отклик на «" + Html.escape(Html.truncate(kwork.getName(), 80)) + "»</b>\n\n"
                + "<blockquote>" + Html.escape(proposal.getText()) + "</blockquote>";
    }

    /** @param variants все варианты пользователя на этот заказ по порядку, содержат proposal */
    public InlineKeyboardMarkup keyboard(Proposal proposal, List<Proposal> variants) {
        Long kworkId = proposal.getKworkId();
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                Keyboard.button("📄 Заказ", "kwork", "card:" + kworkId),
                Keyboard.button("🔄 Ещё вариант", "reply", "gen:" + kworkId)));

        int index = indexOf(variants, proposal);
        if (variants.size() > 1 && index >= 0) {
            rows.add(new InlineKeyboardRow(
                    arrow("◀️", variants, index - 1),
                    Keyboard.button((index + 1) + "/" + variants.size(), "reply", NOOP),
                    arrow("▶️", variants, index + 1)));
        }
        rows.add(new InlineKeyboardRow(Keyboard.urlButton("🔗 Открыть на Kwork", KworkCardFormatter.url(kworkId))));
        return Keyboard.keyboard(rows);
    }

    /** Пока идёт генерация: одна неактивная кнопка, чтобы не нажали повторно. */
    public InlineKeyboardMarkup loading() {
        return Keyboard.keyboard(Keyboard.button("⏳ Генерирую отклик…", "reply", NOOP));
    }

    private static InlineKeyboardButton arrow(String text, List<Proposal> variants, int index) {
        if (index < 0 || index >= variants.size()) {
            return Keyboard.button("·", "reply", NOOP);
        }
        return Keyboard.button(text, "reply", "show:" + variants.get(index).getId());
    }

    private static int indexOf(List<Proposal> variants, Proposal proposal) {
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).getId().equals(proposal.getId())) {
                return i;
            }
        }
        return -1;
    }
}
