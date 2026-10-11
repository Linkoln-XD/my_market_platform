# Demo UI (Vite + React)

Storefront и минимальная admin-панель поверх REST API `order-service` (8080) и `notification-service` (8081).

## Что внутри

| Маршрут | Описание |
|---------|----------|
| `/` | Каталог (публичный GET products) |
| `/cart` | Корзина по `ownerId` из JWT |
| `/order/:id` | Статус заказа, polling до **PAID** / terminal state |
| `/admin/products` | Изменение stock (ADMIN) |
| `/admin/orders` | Список всех заказов (ADMIN) |
| `/admin/notifications` | Журнал notification-service (ADMIN) |

Прокси:

- **Vite dev:** `/api` → `localhost:8080`, `/notification-api` → `localhost:8081` (rewrite в `/api/...`)
- **nginx (Docker):** то же через `host.docker.internal` — см. `frontend/nginx.conf`

JWT: кнопки «Войти как покупатель» / «admin» вызывают `POST /api/v1/auth/demo-token` на order-service. Для admin notifications нужен тот же `JWT_SECRET`, что у notification-service.

## Быстрый старт (Vite, рекомендуется)

```bash
docker compose up -d

./mvnw -pl order-service -am -DskipTests install
./mvnw -pl notification-service -am -DskipTests install

# терминал A
./mvnw -pl order-service spring-boot:run -Dspring-boot.run.profiles=local

# терминал B
./mvnw -pl notification-service spring-boot:run -Dspring-boot.run.profiles=local

cd frontend && npm install && npm run dev
```

Открыть http://localhost:5173

## Docker UI (без Node на хосте)

Backend **обязательно на хосте** (контейнер UI проксирует на `host.docker.internal:8080` и `:8081`):

```bash
docker compose up -d
# ... запустить order-service и notification-service как выше ...

docker compose --profile demo up -d --build demo-ui
```

http://localhost:3000

Сборка фронта выполняется внутри `frontend/Dockerfile` (multi-stage: `npm run build` + nginx).

## Остановка

```bash
# Ctrl+C — Java-процессы в терминалах

lsof -ti :8080,:8081 | xargs kill   # если нужно принудительно

docker compose --profile demo stop demo-ui
docker compose down                  # Postgres/Redis/Kafka
```

## Устранение неполадок

### Каталог пустой или «ошибка API»

1. Проверьте backend:  
   `curl -s -o /dev/null -w '%{http_code}\n' 'http://localhost:8080/api/v1/products?size=1'`  
   Ожидание: **200**.

2. **401** на каталог при живом order-service часто означает, что контроллер упал с **500**, а Spring Boot отдал **401 на `/error`** (endpoint `/error` без JWT). Смотрите логи order-service.

3. **500** на `/api/v1/products` после обновления кода — типично **устаревшие записи Redis-кэша** каталога (`@Cacheable`). Сброс:

   ```bash
   docker exec market-redis redis-cli FLUSHDB
   ```

4. Docker UI не видит API: убедитесь, что 8080/8081 слушают на **хосте**, а не только внутри другого контейнера.

### Admin notifications 401/403

- Токен с ролью **ADMIN**
- `JWT_SECRET` одинаковый в `.env` / окружении для обоих сервисов
- Запрос идёт на `/notification-api/v1/admin/notifications` (прокси на 8081)

### CORS

Разрешены origin: `http://localhost:5173`, `http://localhost:3000` (`market.cors.allowed-origins` в `application.yml`).

## Связанные документы

- [e2e-local-flow.md](e2e-local-flow.md) — сценарий без UI (curl)
- [README.md](../README.md) — обзор API и security
