package org.example.telegramkworkbot.message;

public final class Html {
    private Html() {}

    /** Экранирование для parse_mode=HTML в Telegram. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Обрезает текст до max символов, добавляя многоточие. */
    public static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 1).stripTrailing() + "…";
    }
}
