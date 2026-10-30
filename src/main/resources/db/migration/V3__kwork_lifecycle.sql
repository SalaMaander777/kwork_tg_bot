-- Жизненный цикл заказа. Таблицей kwork владеет парсер и использует те же колонки,
-- IF NOT EXISTS делает миграцию безопасной при любом порядке выкладки.
alter table kwork add column if not exists first_seen_at timestamp default now();
alter table kwork add column if not exists last_seen_at  timestamp default now();
alter table kwork add column if not exists is_active     boolean not null default true;
alter table kwork add column if not exists closed_at     timestamp;

create index if not exists idx_kwork_active_id on kwork (kwork_order_id) where is_active;
