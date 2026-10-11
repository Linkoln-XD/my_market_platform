# E2E: локальный сценарий (Compose + оба сервиса)

Проверка полного пути: каталог → корзина → заказ → Kafka (оплата, stock) → notification → отмена.

## 1. Инфраструктура

```bash
docker compose up -d
# опционально Prometheus (scrape с хоста, см. infra/prometheus/prometheus.yml):
docker compose --profile observability up -d
```

JDK 21, переменные — `.env.example` (`JWT_SECRET`, `KAFKA_BOOTSTRAP_SERVERS`, …).

## 2. Сборка и запуск приложений

Терминал A — order-service:

```bash
./mvnw -pl order-service -am -DskipTests install
./mvnw -pl order-service spring-boot:run -Dspring-boot.run.profiles=local
```

Терминал B — notification-service:

```bash
./mvnw -pl notification-service -am -DskipTests install
./mvnw -pl notification-service spring-boot:run -Dspring-boot.run.profiles=local
```

Readiness:

```bash
curl -s http://localhost:8080/actuator/health/readiness | jq .
curl -s http://localhost:8081/actuator/health/readiness | jq .
```

Ожидание: `db`, `redis`, `kafka` — **UP** (order); `db`, `kafka` — **UP** (notification).

## 3. JWT (demo)

```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/demo-token \
  -H 'Content-Type: application/json' \
  -d '{"subject":"e2e-user","roles":["USER"]}' | jq -r .accessToken)
echo "$TOKEN" | head -c 20 && echo '…'
```

`ownerId` в заказе **должен совпадать** с `subject` (`e2e-user`).

## 4. Каталог и корзина

```bash
curl -s 'http://localhost:8080/api/v1/products?size=5' | jq '.content[0] | {id,name,stockQuantity}'

export PRODUCT_ID=22222222-2222-2222-2222-222222222201

curl -s -X POST "http://localhost:8080/api/v1/carts/e2e-user/items" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":1}" | jq .
```

## 5. Checkout (outbox → order.events)

```bash
export IDEM_KEY="e2e-checkout-$(date +%s)"

curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: e2e-req-001' \
  -d '{"ownerId":"e2e-user"}' | jq .
```

Статус заказа: **PENDING_PAYMENT**. В логах order-service — outbox publish; mock payment переводит заказ в **PAID** (обычно несколько секунд).

Проверка статуса в БД:

```bash
docker exec -it market-postgres psql -U market -d market -c \
  "select id, status, owner_id from ordering.orders order by created_at desc limit 3;"
```

## 6. Notification

После **OrderPaid** в логах notification-service:

- `NOTIFY [OrderPaid]`
- `EMAIL_STUB`

```bash
docker exec -it market-postgres psql -U market -d market -c \
  "select event_type, order_id, channel, delivered_at from notification.delivered_notifications order by delivered_at desc limit 5;"
```

## 7. Observability

Prometheus (если поднят профиль `observability`):

- UI: http://localhost:9090
- Targets: `order-service`, `notification-service` → **UP**

Метрики на хосте:

```bash
curl -s http://localhost:8080/actuator/prometheus | grep -E 'market_orders_created|jvm_memory'
curl -s http://localhost:8081/actuator/prometheus | grep -E 'market_notifications_delivered|jvm_memory'
```

Корреляция: заголовок **`X-Request-Id`** возвращается в ответе; в логах — `[req:… evt:…]`.

## 8. Отмена оплаченного заказа (saga)

Подставьте `ORDER_ID` из шага 5:

```bash
curl -s -X POST "http://localhost:8080/api/v1/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"reason":"E2E cancel"}' | jq .
```

Ожидание: статус **CANCELLED**, в Kafka — `OrderCancelled`, компенсации stock/refund (см. [saga-order-flow.md](saga-order-flow.md)).

## 9. Автоматическая проверка

```bash
./mvnw verify
```

Testcontainers: Postgres, Redis, Kafka; IT покрывают API, outbox, payment, cancel saga, security, notification, Prometheus.
