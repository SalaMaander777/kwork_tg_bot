package org.example.telegramkworkbot.callback;

public record CallbackData(String action, String payload) {
    public static CallbackData parse(String raw){
        int i = raw.indexOf(":");
        return i < 0
                ? new CallbackData(raw, "")
                : new CallbackData(raw.substring(0, i), raw.substring(i + 1));
    }
    public String encode() {
        return action + ":" + payload;
    }
}
