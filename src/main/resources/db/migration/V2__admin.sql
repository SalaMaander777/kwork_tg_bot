-- Данные для админ-панели: активность пользователей, доставки, вызовы LLM, события, настройки, рассылки, аудит.

alter table bot_users
    add column last_activity_at     timestamp(6) with time zone,
    add column banned               boolean not null default false,
    add column daily_limit_override integer;

alter table notifier_state
    add column last_run_at timestamp(6) with time zone;

-- каждая отправленная пользователю карточка заказа
create table deliveries (
    id       bigserial primary key,
    user_id  bigint      not null,
    kwork_id bigint      not null,
    sent_at  timestamp(6) with time zone not null,
    status   varchar(16) not null -- OK | BLOCKED | FAILED
);
create index idx_deliveries_sent on deliveries (sent_at);
create index idx_deliveries_user on deliveries (user_id);
create index idx_deliveries_kwork on deliveries (kwork_id);

-- каждый вызов языковой модели
create table llm_calls (
    id                bigserial primary key,
    user_id           bigint,
    kwork_id          bigint,
    model             varchar(64),
    started_at        timestamp(6) with time zone not null,
    duration_ms       integer     not null,
    status            varchar(16) not null, -- OK | ERROR
    prompt_tokens     integer,
    completion_tokens integer,
    error             text
);
create index idx_llm_calls_started on llm_calls (started_at);

alter table proposals
    add column llm_call_id bigint;

-- события воронки и действий пользователей
create table bot_events (
    id         bigserial primary key,
    user_id    bigint,
    type       varchar(32) not null,
    created_at timestamp(6) with time zone not null,
    payload    text
);
create index idx_bot_events_created on bot_events (created_at);
create index idx_bot_events_user on bot_events (user_id);
create index idx_bot_events_type on bot_events (type, created_at);

-- настройки, изменяемые из админки (поверх application.yaml)
create table app_settings (
    key        varchar(64) primary key,
    value      text        not null,
    updated_at timestamp(6) with time zone not null
);

create table broadcasts (
    id          bigserial primary key,
    text        text        not null,
    segment     text        not null, -- JSON с условиями отбора
    status      varchar(16) not null, -- RUNNING | DONE | CANCELLED | FAILED
    total       integer     not null default 0,
    sent        integer     not null default 0,
    failed      integer     not null default 0,
    created_at  timestamp(6) with time zone not null,
    finished_at timestamp(6) with time zone
);

create table admin_audit (
    id         bigserial primary key,
    action     varchar(64) not null,
    target     varchar(128),
    details    text,
    ip         varchar(64),
    created_at timestamp(6) with time zone not null
);
create index idx_admin_audit_created on admin_audit (created_at);
