package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.example.telegramkworkbot.entities.KworkEntity;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.Html;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/** Карточка заказа для рассылки. */
@Component
@RequiredArgsConstructor
public class KworkCardFormatter {

    /** Лимит Telegram на текст сообщения (считается по символам после разбора разметки). */
    static final int TELEGRAM_MAX_LENGTH = 4096;
    /** Запас под многоточие и расхождения в подсчёте длины. */
    private static final int LENGTH_MARGIN = 50;

    private final CategoryCatalog catalog;

    public static String url(Long kworkId) {
        return "https://kwork.ru/projects/" + kworkId;
    }

    public String text(KworkEntity kwork) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(Html.escape(plain(kwork.getName()))).append("</b>\n");

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
        String footer = kwork.getUsername() != null ? "\n👤 " + Html.escape(kwork.getUsername()) : "";
        String description = plain(kwork.getDescription()).strip();
        if (!description.isEmpty()) {
            // Описание занимает всё свободное место в пределах лимита Telegram, а не фиксированные N символов.
            // Длину считаем по тексту без тегов; экранирование (&amp; и т.п.) Telegram разворачивает обратно.
            int used = visibleLength(sb) + visibleLength(footer) + 2;
            int budget = Math.max(0, TELEGRAM_MAX_LENGTH - LENGTH_MARGIN - used);
            sb.append('\n').append(Html.escape(Html.truncate(description, budget))).append('\n');
        }
        return sb.append(footer).toString();
    }

    /** @param latestProposalId последний сгенерированный отклик на заказ, если есть */
    public InlineKeyboardMarkup keyboard(KworkEntity kwork, Optional<Long> latestProposalId) {
        InlineKeyboardButton reply = latestProposalId
                .map(id -> Keyboard.button("📝 Мой отклик", "reply", "show:" + id))
                .orElseGet(() -> Keyboard.button("✍️ Отклик", "reply", "gen:" + kwork.getId()));
        return Keyboard.keyboard(
                reply,
                Keyboard.urlButton("🔗 Открыть", url(kwork.getId())),
                Keyboard.button("🙈 Скрыть", "kwork", "hide"));
    }

    /** Заказы приходят с HTML-сущностями (&quot;, &amp; ...) — превращаем в обычный текст, чтобы не экранировать дважды. */
    static String plain(String text) {
        return text == null ? "" : HtmlUtils.htmlUnescape(text);
    }

    private static int visibleLength(CharSequence html) {
        return HtmlUtils.htmlUnescape(html.toString().replaceAll("</?[a-z]+>", "")).length();
    }

    private static String price(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }
}
