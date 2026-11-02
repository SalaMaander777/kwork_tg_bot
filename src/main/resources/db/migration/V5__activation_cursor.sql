-- Рассылка идёт по времени появления заказа (activated_at), а не по id: id не растёт в порядке
-- публикации (модерация), а старые заказы заказчики поднимают заново.
-- Колонку ведёт парсер; IF NOT EXISTS делает миграцию безопасной при любом порядке выкладки.
do $$
begin
    if to_regclass('public.kwork') is not null then
        alter table kwork add column if not exists activated_at timestamptz default now();
    end if;
end
$$;

alter table notifier_state add column last_activated_at timestamptz;
-- всё, что уже есть в базе, повторно не рассылаем: курсор встаёт на текущий момент
update notifier_state set last_activated_at = now();
