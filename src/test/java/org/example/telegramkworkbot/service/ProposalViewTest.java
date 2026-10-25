package org.example.telegramkworkbot.service;

import org.example.telegramkworkbot.entities.Proposal;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalViewTest {

    private final ProposalView view = new ProposalView();

    @Test
    void textIsEscapedQuote() {
        String text = view.text(KworkCardFormatterTest.kwork("Бот <Telegram>", "y"), proposal(1L, "Сделаю за 3 дня & без предоплаты"));

        assertThat(text).startsWith("✍️ <b>Отклик на «Бот &lt;Telegram&gt;»</b>")
                .contains("<blockquote>Сделаю за 3 дня &amp; без предоплаты</blockquote>");
    }

    @Test
    void singleVariantHasNoPager() {
        Proposal only = proposal(1L, "a");
        List<InlineKeyboardRow> rows = view.keyboard(only, List.of(only)).getKeyboard();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("kwork:card:42", "reply:gen:42");
    }

    @Test
    void pagerPointsToNeighbours() {
        Proposal first = proposal(10L, "a");
        Proposal second = proposal(11L, "b");
        Proposal third = proposal(12L, "c");

        InlineKeyboardRow pager = view.keyboard(second, List.of(first, second, third)).getKeyboard().get(1);

        assertThat(pager).extracting(InlineKeyboardButton::getText).containsExactly("◀️", "2/3", "▶️");
        assertThat(pager).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("reply:show:10", "reply:noop", "reply:show:12");
    }

    @Test
    void edgesAreInactive() {
        Proposal first = proposal(10L, "a");
        Proposal second = proposal(11L, "b");

        InlineKeyboardRow pager = view.keyboard(first, List.of(first, second)).getKeyboard().get(1);

        assertThat(pager.get(0).getCallbackData()).isEqualTo("reply:noop");
        assertThat(pager.get(2).getCallbackData()).isEqualTo("reply:show:11");
    }

    private static Proposal proposal(Long id, String text) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setUserId(1L);
        proposal.setKworkId(42L);
        proposal.setText(text);
        return proposal;
    }
}
