package org.example.telegramkworkbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.callback.CallbackData;
import org.example.telegramkworkbot.callback.CallbackHandler;
import org.example.telegramkworkbot.dto.KworkCategoryDto;
import org.example.telegramkworkbot.dto.KworkSubcategoryDto;
import org.example.telegramkworkbot.enums.UserState;
import org.example.telegramkworkbot.keyboard.Keyboard;
import org.example.telegramkworkbot.message.CallbackAnswer;
import org.example.telegramkworkbot.message.EditMessage;
import org.example.telegramkworkbot.message.Html;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.message.TextMessage;
import org.example.telegramkworkbot.sender.MessageSender;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Выбор категорий. Сам является CallbackHandler для действия "category",
 * поэтому отдельная обёртка CategoryCallbackHandler больше не нужна.
 *
 * Payload кнопок: root | open:&lt;rootId&gt; | toggle:&lt;subId&gt; | all:&lt;rootId&gt; | done
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySelectionService implements CallbackHandler {

    private static final String ACTION = "category";
    private static final String ROOT_TEXT = "Выберите категории, заказы из которых хотите получать:";

    private final CategoryCatalog catalog;
    private final UserService userService;
    private final ProfileService profileService;
    private final MessageSender sender;

    @Override
    public String action() {
        return ACTION;
    }

    /** Вызывается из онбординга и /categories: показать первое меню выбора категорий. */
    public void start(Long chatId, Long userId) {
        Set<Long> selected = userService.getSubcategories(userId);
        sender.send(new KeyboardMessage(chatId, ROOT_TEXT, rootMenu(selected)));
    }

    /** Нажатие на кнопку категории/подкатегории. */
    @Override
    public void handle(CallbackQuery query, CallbackData data) {
        Long userId = query.getFrom().getId();
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();
        CallbackData op = CallbackData.parse(data.payload());

        switch (op.action()) {
            case "root" -> showRoot(chatId, messageId, userId);
            case "open" -> catalog.root(Long.parseLong(op.payload()))
                    .ifPresent(root -> showSubs(root, chatId, messageId, userId));
            case "toggle" -> {
                long subId = Long.parseLong(op.payload());
                if (catalog.sub(subId).isPresent()) {
                    userService.toggleSubcategory(userId, subId);
                }
                rootOf(subId).ifPresent(root -> showSubs(root, chatId, messageId, userId));
            }
            case "all" -> catalog.root(Long.parseLong(op.payload())).ifPresent(root -> {
                userService.toggleAll(userId, catalog.subIds(root));
                showSubs(root, chatId, messageId, userId);
            });
            case "done" -> {
                if (!finish(chatId, messageId, userId)) {
                    sender.send(new CallbackAnswer(chatId, query.getId(), "Выберите хотя бы одну подкатегорию"));
                    return;
                }
            }
            default -> log.debug("Unknown category op: {}", op.action());
        }
        sender.send(new CallbackAnswer(chatId, query.getId(), null));
    }

    private boolean finish(Long chatId, Integer messageId, Long userId) {
        Set<Long> selected = userService.getSubcategories(userId);
        if (selected.isEmpty()) {
            return false;
        }
        sender.send(new EditMessage(chatId, messageId, summary(selected), null));
        if (userService.getState(userId) == UserState.CHOOSING_CATEGORY) {
            profileService.askProfile(chatId, userId);
        } else {
            sender.send(new TextMessage(chatId, "Категории сохранены 👍"));
        }
        return true;
    }

    private void showRoot(Long chatId, Integer messageId, Long userId) {
        Set<Long> selected = userService.getSubcategories(userId);
        sender.send(new EditMessage(chatId, messageId, ROOT_TEXT, rootMenu(selected)));
    }

    private void showSubs(KworkCategoryDto root, Long chatId, Integer messageId, Long userId) {
        Set<Long> selected = userService.getSubcategories(userId);
        String text = "<b>" + Html.escape(root.getName()) + "</b>\n"
                + "Отметьте подходящие подкатегории (выбрано всего: " + selected.size() + ").\n"
                + "Когда закончите — нажмите «✅ Готово» внизу.";
        sender.send(new EditMessage(chatId, messageId, text, subMenu(root, selected)));
    }

    private Optional<KworkCategoryDto> rootOf(long subId) {
        return catalog.roots().stream()
                .filter(root -> catalog.subIds(root).contains(subId))
                .findFirst();
    }

    private InlineKeyboardMarkup rootMenu(Set<Long> selected) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (KworkCategoryDto root : catalog.roots()) {
            long count = catalog.subIds(root).stream().filter(selected::contains).count();
            String label = count > 0 ? root.getName() + " (" + count + ")" : root.getName();
            buttons.add(Keyboard.button(label, ACTION, "open:" + root.getId()));
        }
        List<InlineKeyboardRow> rows = Keyboard.grid(buttons, 2);
        rows.add(new InlineKeyboardRow(Keyboard.button("✅ Готово", ACTION, "done")));
        return Keyboard.keyboard(rows);
    }

    private InlineKeyboardMarkup subMenu(KworkCategoryDto root, Set<Long> selected) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (KworkSubcategoryDto sub : root.getCats()) {
            String mark = selected.contains(sub.getId()) ? "✅ " : "";
            rows.add(new InlineKeyboardRow(Keyboard.button(mark + sub.getName(), ACTION, "toggle:" + sub.getId())));
        }
        boolean allSelected = selected.containsAll(catalog.subIds(root));
        rows.add(new InlineKeyboardRow(
                Keyboard.button(allSelected ? "Снять все" : "Выбрать все", ACTION, "all:" + root.getId())));
        rows.add(new InlineKeyboardRow(
                Keyboard.button("⬅️ Другие категории", ACTION, "root"),
                Keyboard.button("✅ Готово", ACTION, "done")));
        return Keyboard.keyboard(rows);
    }

    private String summary(Set<Long> selected) {
        StringBuilder sb = new StringBuilder("Вы выбрали:\n");
        for (KworkCategoryDto root : catalog.roots()) {
            List<String> names = root.getCats().stream()
                    .filter(sub -> selected.contains(sub.getId()))
                    .map(KworkSubcategoryDto::getName)
                    .toList();
            if (!names.isEmpty()) {
                sb.append("\n<b>").append(Html.escape(root.getName())).append("</b>: ")
                        .append(Html.escape(String.join(", ", names)));
            }
        }
        return sb.toString();
    }
}
