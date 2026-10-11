# Архитектурные решения

Зафиксировано на шаге 0. Меняется только после явного согласия.

## Сборка

Maven multi-module, Java 21, Spring Boot 3.5.16.

Рассмотренные варианты сборки:

1. **Maven multi-module — выбран.** Привычная схема для enterprise Java, команда проверки `./mvnw verify`, отдельный артефакт `common-events` для будущего notification-service.
2. Gradle (Kotlin DSL) — быстрее инкрементальная сборка, для ревьюеров портфолио читается хуже.
3. Один Maven-модуль — `common-events` нельзя было бы подключить вторым сервисом без выноса.

## Что запускается сейчас

| Модуль | Роль сейчас | Когда меняется |
|--------|-------------|----------------|
| `common-events` | Общий jar контрактов Kafka. Типы событий — с шага 4 | Контракты растут вместе с топиками |
| `order-service` | Единственный deployable: каталог, корзина, заказ, outbox, saga, mock payment | Mock payment остаётся здесь |
| `notification-service` | Есть (8081) | Consumer только `order.events`, схема `notification`, DLQ `order.events.dlq` |

Каталог хранится в `order-service` (схема `catalog`). Это одна БД и один процесс, но граница резерва stock — Kafka consumer, а не вызов метода и не `@EventListener`.

## Брокер

Apache Kafka **3.9.2**, режим **KRaft**, образ `apache/kafka:3.9.2`. ZooKeeper нет. Версия совпадает с `kafka-clients` из Spring Boot 3.5.16.

Рассмотренные варианты брокера:

1. **Apache Kafka KRaft — выбран.** Прямое требование стека, клиент и брокер одной линейки 3.9.
2. Redpanda (Kafka API) — легче по памяти, в портфолио это уже другой продукт.
3. Kafka + ZooKeeper — для нового проекта устаревшая схема.

Профиль `local` ходит в брокер из Compose. Профиль `test` поднимает Kafka через Testcontainers. In-memory замены нет. Spring Boot 3.5 сам не регистрирует health contributor Kafka, а `KafkaAdmin` с `fail-fast` проверяет брокер только при создании топиков. Поэтому `KafkaHealthIndicator` описывает кластер на старте и на каждом probe: нет брокера — процесс не стартует, readiness остаётся DOWN.

## Топики

Пока только контракт. Продюсеров и consumer-ов бизнес-потока ещё нет (шаг 4+).

| Топик | Кто публикует | Кто читает |
|-------|----------------|------------|
| `order.events` | outbox в `order-service` (`OrderCreated`, `OrderPaid`, `OrderCancelled`, …) | payment-поток внутри `order-service`; с шага 7 — `notification-service` |
| `payment.events` | mock payment consumers в `order-service` | consumer заказа в `order-service` |
| `catalog.events` | catalog consumer (`StockReserved`, `StockReleased`) | consumer заказа в `order-service` |

У каждого доменного события будут `eventId` (UUID) и `occurredAt`. Формат — JSON в `common-events`.

Доменные события заказа и оплаты пишутся в `ordering.outbox` в той же транзакции, что и изменение состояния. Consumer записывает `eventId` в `processed_events` и игнорирует повтор.

## Stock

Резерв и возврат остатка идут через `catalog.events`.

Целевой поток:

1. `POST /orders` в одной транзакции пишет заказ `PENDING_PAYMENT`, позиции и строку outbox (`OrderCreated`). Остаток в этой транзакции не меняется.
2. Publisher (шаг 4) отправляет `OrderCreated` в `order.events`.
3. Catalog consumer резервирует stock (optimistic locking) и публикует `StockReserved` или отказ в `catalog.events`.
4. После `StockReserved` payment-поток публикует `PaymentRequested` в `payment.events`.
5. Mock gateway публикует `PaymentSucceeded` или `PaymentFailed`.
6. Consumer заказа переводит статус и через outbox публикует `OrderPaid`, либо запускает компенсацию: `StockReleased` и `OrderCancelled`.

Optimistic locking на stock живёт в catalog consumer, не в синхронном методе создания заказа.

## Данные

- PostgreSQL 16. Схемы `catalog` и `ordering`. История Flyway — в `public`.
- `ddl-auto=none`. Схема меняется только миграциями Flyway.
- Redis 7: корзина, кэш каталога, rate limit, ключи идемпотентности с коротким TTL (со следующих шагов).
- `processed_events` и `outbox` появятся в схеме `ordering` на шагах 4–5.

## Безопасность (шаг 8)

- **JWT resource server** (HS256, issuer `market.security.jwt.issuer`).
- **Demo issuer**: `POST /api/v1/auth/demo-token` (local/test, `market.security.demo-token.enabled`).
- **Публично**: GET каталог, все операции корзины, health, OpenAPI.
- **Authenticated**: `POST/PUT` каталог — роль **ADMIN**; `POST /api/v1/orders` и cancel — любой JWT; `ownerId` в body = `sub` токена.
- **Rate limit**: Redis, `POST /api/v1/orders` — `market.security.rate-limit.*` (по умолчанию 30/min на subject).

## Observability (шаг 9)

- **Micrometer + Prometheus**: `/actuator/prometheus`, `/actuator/metrics` (local — без JWT; в prod вынести за ingress/mTLS).
- **Бизнес-метрики**: `market.orders.created`, `market.notifications.delivered` (tag `event_type`).
- **Корреляция**: `X-Request-Id` → MDC `requestId`; Kafka consumer notification — MDC `eventId`.
- **Логи**: `logback-spring.xml`, поля `[req:… evt:…]` в консоли.
- **Prometheus в Compose**: профиль `observability`, конфиг `infra/prometheus/prometheus.yml` (scrape `host.docker.internal:8080/8081`).
- **E2E сценарий**: [e2e-local-flow.md](e2e-local-flow.md).

## Профили

| Профиль | Инфраструктура |
|---------|----------------|
| `local` | Docker Compose: Postgres, Redis, Kafka. Без брокера процесс не стартует |
| `test` | Testcontainers: Postgres, Redis, Kafka |

Секретов в репозитории нет. Локальные значения по умолчанию лежат в `.env.example` и `application-local.yml` (пароль БД `market` только для Compose).

## Lombok и MapStruct

С шага 1, когда появятся сущности и DTO. Lombok — единообразно на boilerplate. MapStruct — на границах DTO и entity. На шаге 0 зависимостей нет: маппить нечего.
