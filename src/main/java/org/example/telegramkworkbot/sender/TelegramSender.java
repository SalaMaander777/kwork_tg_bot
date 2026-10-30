package org.example.telegramkworkbot.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramkworkbot.message.KeyboardMessage;
import org.example.telegramkworkbot.message.OutgoingMessage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;


@Slf4j
@RequiredArgsConstructor
@Component
public class TelegramSender implements MessageSender{
    private static final int MAX_RETRY_AFTER_SECONDS = 30;

    private final TelegramClient client;

    @Override
    public SendStatus send(OutgoingMessage message) {
        try {
            message.deliver(client);
            return SendStatus.OK;
        } catch (TelegramApiRequestException e) {
            return handleRequestError(message, e);
        } catch (TelegramApiException e) {
           log.error("Ошибка сообщения в чат: {}", message.chatId(), e);
           return SendStatus.FAILED;
        }
    }

    @Override
    public Sent sendTracked(KeyboardMessage message) {
        try {
            return new Sent(SendStatus.OK, message.deliverAndGet(client).getMessageId());
        } catch (TelegramApiRequestException e) {
            return new Sent(handleRequestError(message, e), null);
        } catch (TelegramApiException e) {
            log.error("Ошибка сообщения в чат: {}", message.chatId(), e);
            return new Sent(SendStatus.FAILED, null);
        }
    }

    private SendStatus handleRequestError(OutgoingMessage message, TelegramApiRequestException e) {
        Integer code = e.getErrorCode();
        if (code != null && code == 403) {
            log.info("Чат {} недоступен (бот заблокирован)", message.chatId());
            return SendStatus.BLOCKED;
        }
        if (code != null && code == 429 && e.getParameters() != null && e.getParameters().getRetryAfter() != null) {
            // флуд-контроль Telegram: ждём сколько просят и пробуем ещё раз
            int retryAfter = Math.min(e.getParameters().getRetryAfter(), MAX_RETRY_AFTER_SECONDS);
            log.warn("429 от Telegram, повтор через {} c", retryAfter);
            try {
                Thread.sleep(retryAfter * 1000L);
                message.deliver(client);
                return SendStatus.OK;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (TelegramApiException retryError) {
                log.error("Повторная отправка в чат {} не удалась", message.chatId(), retryError);
            }
            return SendStatus.FAILED;
        }
        if (e.getApiResponse() != null && e.getApiResponse().contains("message is not modified")) {
            return SendStatus.OK; // повторное нажатие той же кнопки — не ошибка
        }
        log.error("Ошибка сообщения в чат: {}", message.chatId(), e);
        return SendStatus.FAILED;
    }
}
