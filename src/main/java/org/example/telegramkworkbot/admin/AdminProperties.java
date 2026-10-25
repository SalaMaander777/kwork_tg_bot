package org.example.telegramkworkbot.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Доступ в админ-панель.
 *
 * @param passwordHash bcrypt-хэш пароля (предпочтительно)
 * @param password     пароль открытым текстом — если хэш не задан; хэшируется при старте
 * @param telegramId   Telegram ID администратора: техработы его не касаются, ему уходят тестовые рассылки
 */
@ConfigurationProperties("admin")
public record AdminProperties(
        String username,
        String passwordHash,
        String password,
        Long telegramId
) {
    public boolean enabled() {
        return username != null && !username.isBlank()
                && ((passwordHash != null && !passwordHash.isBlank()) || (password != null && !password.isBlank()));
    }

    public boolean isAdmin(Long userId) {
        return telegramId != null && telegramId.equals(userId);
    }
}
