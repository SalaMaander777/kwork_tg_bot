package org.example.telegramkworkbot.settings;

import java.util.List;

/**
 * Описание настройки, изменяемой из админки.
 *
 * @param options допустимые значения для CHOICE (для остальных типов пусто)
 */
public record SettingDef(
        String key,
        Type type,
        String group,
        String label,
        String description,
        String defaultValue,
        Double min,
        Double max,
        List<String> options
) {
    public enum Type { BOOLEAN, INTEGER, DECIMAL, TEXT, CHOICE }

    /** @return текст ошибки или null, если значение допустимо */
    public String validate(String value) {
        if (value == null) {
            return "Значение не задано";
        }
        try {
            switch (type) {
                case BOOLEAN -> {
                    if (!value.equals("true") && !value.equals("false")) return "Ожидается true или false";
                }
                case INTEGER -> {
                    return checkRange(Long.parseLong(value.strip()));
                }
                case DECIMAL -> {
                    return checkRange(Double.parseDouble(value.strip()));
                }
                case CHOICE -> {
                    if (!options.contains(value)) return "Допустимые значения: " + String.join(", ", options);
                }
                case TEXT -> {
                    if (value.isBlank()) return "Значение не может быть пустым";
                    if (max != null && value.length() > max) return "Не длиннее " + max.intValue() + " символов";
                }
            }
        } catch (NumberFormatException e) {
            return "Ожидается число";
        }
        return null;
    }

    private String checkRange(double number) {
        if (min != null && number < min) return "Не меньше " + format(min);
        if (max != null && number > max) return "Не больше " + format(max);
        return null;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
