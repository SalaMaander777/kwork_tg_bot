# Kwork AI Bot

Telegram-бот для фрилансеров Kwork: присылает новые заказы из выбранных категорий и помогает написать отклик с помощью AI (GigaChat).

> Текущая версия — **MVP** (`v0.1.0-mvp`).

## Возможности

- **Выбор категорий.** Удобное меню с категориями и подкатегориями Kwork: ✅ у выбранных, «Выбрать все», «Готово».
- **Профиль фрилансера.** Короткий рассказ о себе (опыт, стек, портфолио) — на его основе AI пишет отклики.
- **Уведомления о новых заказах.** Карточка с бюджетом, сроком, описанием и числом откликов; кнопки «✍️ Отклик», «🔗 Открыть», «🙈 Скрыть».
- **AI-отклики.** Отклик генерируется прямо в сообщении с заказом:
  - несколько вариантов с листанием ◀️ 2/3 ▶️;
  - переключение «📄 Заказ» ↔ «📝 Мой отклик»;
  - дневной лимит генераций на пользователя.

## Как это устроено

```
┌──────────┐   пишет    ┌────────────┐   читает   ┌──────────────────┐   Telegram   ┌──────────────┐
│  Парсер  │──────────▶│ PostgreSQL │◀──────────│  Kwork AI Bot    │─────────────▶│ Пользователь │
│(отдельно)│  kwork    │            │  bot_*     │ (Spring Boot)    │              └──────────────┘
└──────────┘            └────────────┘            │        │         │
                                                  │        ▼         │
                                                  │   GigaChat API   │
                                                  └──────────────────┘
```

- **Парсер** — отдельный сервис (не в этом репозитории), складывает заказы Kwork в таблицу `kwork`.
- **Бот** раз в минуту забирает новые заказы (`id` больше последнего обработанного) и рассылает их подписчикам подходящих подкатегорий.
- `kwork.category_id` — это id подкатегории из `src/main/resources/kwork_categories.json`.
- Таблицы бота создаёт **Flyway**. Таблицу парсера Hibernate только проверяет (`ddl-auto: validate`) и не изменяет.

## Стек

- Java 17+, Spring Boot 4, Spring Data JPA, Flyway
- PostgreSQL
- [TelegramBots](https://github.com/rubenlagus/TelegramBots) 10 (long polling)
- GigaChat API (HTTP-клиент на OkHttp), абстракция `LlmClient` для подключения других моделей

## Быстрый старт

### 1. База данных

Бот работает с той же базой, что и парсер. Для локального запуска:

```bash
docker run -d --name my-postgres -p 5432:5432 \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=mydb \
  postgres:16
```

Миграции применяются автоматически при старте.

### 2. Telegram-бот

Создайте бота у [@BotFather](https://t.me/BotFather) и получите токен.

### 3. GigaChat

1. Зарегистрируйтесь на [developers.sber.ru](https://developers.sber.ru/studio) и создайте проект GigaChat API (для физлиц доступен бесплатный freemium-режим).
2. В настройках проекта нажмите «Получить ключ» и сохраните **Client ID** и **Client Secret** (или **Authorization key**).
3. Серверы GigaChat используют сертификаты НУЦ Минцифры. Файл `src/main/resources/certs/russian_trusted_ca.pem` (корневой + выпускающий сертификат) уже лежит в репозитории. Подробнее — [документация](https://developers.sber.ru/docs/ru/gigachat/certificates).

### 4. Переменные окружения

| Переменная | Обязательна | Описание |
|---|---|---|
| `BOT_TOKEN` | да | Токен Telegram-бота |
| `GIGACHAT_CLIENT_ID` + `GIGACHAT_CLIENT_SECRET` | да* | Ключи GigaChat |
| `GIGACHAT_AUTH_KEY` | да* | Альтернатива: Authorization key целиком |
| `GIGACHAT_MODEL` | нет | `GigaChat-2-Max` (по умолчанию), `GigaChat`, `GigaChat-2-Pro`, `GigaChat-3-Ultra` |
| `DB_URL` | нет | По умолчанию `jdbc:postgresql://localhost:5432/mydb` |
| `DB_USER` / `DB_PASSWORD` | нет | По умолчанию `postgres` / `postgres` |

\* нужен один из двух вариантов.

> ⚠️ Не храните токены в `application.yaml` и не коммитьте их в репозиторий.

В IntelliJ IDEA: **Run → Edit Configurations… → Environment variables**:

```
BOT_TOKEN=...;GIGACHAT_CLIENT_ID=...;GIGACHAT_CLIENT_SECRET=...
```

### 5. Запуск

```bash
./mvnw spring-boot:run
```

Или собрать jar:

```bash
./mvnw package -DskipTests
java -jar target/TelegramKworkBot-0.0.1-SNAPSHOT.jar
```

## Команды бота

| Команда | Действие |
|---|---|
| `/start` | Регистрация и онбординг: категории → профиль |
| `/categories` | Изменить категории |
| `/profile` | Посмотреть или изменить описание о себе |
| `/help` | Справка |

## Настройки

Основные параметры в `src/main/resources/application.yaml`:

| Параметр | По умолчанию | Описание |
|---|---|---|
| `kwork.poll-interval` | `PT1M` | Как часто проверять новые заказы |
| `kwork.batch-size` | `100` | Сколько заказов обрабатывать за один проход |
| `ai.daily-limit` | `20` | Генераций откликов на пользователя в день |
| `ai.gigachat.temperature` | `0.8` | «Креативность» модели |
| `ai.gigachat.timeout` | `PT60S` | Таймаут запроса к GigaChat |
| `ai.gigachat.max-concurrent` | `1` | Одновременных запросов (физлицам доступен один поток) |

## Структура проекта

```
src/main/java/org/example/telegramkworkbot/
├── ai/             # LlmClient, GigaChatClient, сертификаты
├── callback/       # Обработчики inline-кнопок (reply:*, kwork:*)
├── commands/       # /start, /categories, /profile, /help
├── configuration/  # Telegram, Jackson, AI
├── entities/       # JPA-сущности (bot_users, proposals, kwork, …)
├── message/        # Исходящие сообщения Telegram
├── router/         # Маршрутизация апдейтов: команды, кнопки, текст
├── sender/         # Отправка в Telegram (403/429)
├── service/        # Бизнес-логика: категории, профиль, рассылка, отклики
└── text/           # Обработка текста по состоянию пользователя
src/main/resources/
├── db/migration/          # Flyway-миграции
├── certs/                 # Сертификаты НУЦ Минцифры
└── kwork_categories.json  # Дерево категорий Kwork
```

## Тесты

```bash
./mvnw test -Dtest='!TelegramKworkBotApplicationTests'
```

`TelegramKworkBotApplicationTests` поднимает весь контекст и требует настоящий `BOT_TOKEN` и запущенную БД.

## Планы

- [ ] AI-оценка соответствия заказа профилю и фильтр по порогу
- [ ] Генерация черновика отклика заранее для подходящих заказов
- [ ] Фильтры: минимальный бюджет, стоп-слова, максимум откликов, тихие часы
- [ ] Статистика: откликнулся / получил заказ
- [ ] Другие провайдеры LLM (YandexGPT, Ollama)
- [ ] Docker Compose для бота и БД
