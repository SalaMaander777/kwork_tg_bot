package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.Html;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Карточка заказа для рассылки. */
@Component
@RequiredArgsConstructor
public class KworkCardFormatter {

    static final int MAX_DESCRIPTION_LENGTH = 700;

    private final CategoryCatalog catalog;

    public static String url(Long kworkId) {
        return "https://kwork.ru/projects/" + kworkId;
    }

    public String text(KworkEntity kwork) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(Html.escape(kwork.getName())).append("</b>\n");

        StringBuilder meta = new StringBuilder();
        if (kwork.getPriceLimit() != null) {
            meta.append("💰 до ").append(price(kwork.getPriceLimit())).append(" ₽");
        }
        if (kwork.getMaxDays() != null) {
            if (!meta.isEmpty()) meta.append(" · ");
            meta.append("⏱ ").append(kwork.getMaxDays()).append(" дн.");
        }
        if (!meta.isEmpty()) meta.append(" · ");
        meta.append("💬 откликов: ").append(kwork.getKworkCount());
        sb.append(meta).append('\n');
        if (kwork.getCategoryId() != null) {
            catalog.sub(kwork.getCategoryId())
                    .map(KworkSubcategoryDto::getName)
                    .ifPresent(name -> sb.append("📂 ").append(Html.escape(name)).append('\n'));
        }
        if (kwork.getDescription() != null && !kwork.getDescription().isBlank()) {
            sb.append('\n').append(Html.escape(Html.truncate(kwork.getDescription().strip(), MAX_DESCRIPTION_LENGTH))).append('\n');
        }
        if (kwork.getUsername() != null) {
            sb.append("\n👤 ").append(Html.escape(kwork.getUsername()));
        }
        return sb.toString();
    }

    public InlineKeyboardMarkup keyboard(KworkEntity kwork) {
        return Keyboard.keyboard(
                Keyboard.button("✍️ Отклик", "reply", "gen:" + kwork.getId()),
                Keyboard.urlButton("🔗 Открыть", url(kwork.getId())),
                Keyboard.button("🙈 Скрыть", "kwork", "hide"));
    }

    private static String price(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }
}
