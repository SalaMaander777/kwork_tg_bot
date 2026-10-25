-- Таблицы бота. Таблица kwork создаётся и наполняется парсером, здесь её не трогаем.

create table bot_users (
    id         bigint primary key, -- id пользователя в Telegram
    username   varchar(255),
    first_name varchar(255),
    last_name  varchar(255),
    created_at timestamp(6) with time zone,
    state      varchar(32) not null
);

create table user_subcategories (
    user_id        bigint not null references bot_users (id) on delete cascade,
    subcategory_id bigint not null,
    primary key (user_id, subcategory_id)
);

create table freelancer_profiles (
    user_id    bigint primary key references bot_users (id) on delete cascade,
    about      text,
    updated_at timestamp(6) with time zone
);

create table proposals (
    id         bigserial primary key,
    user_id    bigint not null references bot_users (id) on delete cascade,
    kwork_id   bigint not null,
    text       text   not null,
    created_at timestamp(6) with time zone not null
);

create index idx_proposals_user_created on proposals (user_id, created_at);

-- Курсор рассылки: id последнего обработанного заказа
create table notifier_state (
    id            integer primary key,
    last_kwork_id bigint not null
);
