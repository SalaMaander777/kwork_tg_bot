-- Отправленные карточки заказов: нужны, чтобы обновлять в них число откликов и статус.
create table card_messages (
    id           bigserial primary key,
    user_id      bigint  not null,
    kwork_id     bigint  not null,
    message_id   integer not null,
    shown_count  integer not null,            -- число откликов, показанное в сообщении сейчас
    shown_closed boolean not null default false, -- в сообщении уже стоит пометка «снят с публикации»
    fail_count   integer not null default 0,
    sent_at      timestamp(6) with time zone not null,
    refreshed_at timestamp(6) with time zone,
    unique (user_id, kwork_id)
);
create index idx_card_messages_sent on card_messages (sent_at);
