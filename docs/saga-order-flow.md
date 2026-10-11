# Saga: order checkout and cancellation

Event-driven flow in `order-service` (modular monolith). Synchronous REST only starts or cancels the order; compensations are Kafka consumers.

## Happy path (checkout + payment)

```mermaid
sequenceDiagram
    participant Client
    participant Order as order-service REST
    participant PG as PostgreSQL
    participant Outbox as OutboxPublisher
    participant OE as order.events
    participant PE as payment.events
    participant Cat as catalog.events

    Client->>Order: POST /orders
    Order->>PG: order PENDING_PAYMENT + outbox OrderCreated
    Outbox->>OE: OrderCreated
    OE->>Order: OrderCreatedPaymentConsumer
    Order->>PE: PaymentRequested
    PE->>Order: MockPaymentGatewayConsumer
    Order->>PE: PaymentSucceeded
    PE->>Order: OrderPaymentResultConsumer
    Order->>PG: order PAID + outbox OrderPaid
    Outbox->>OE: OrderPaid
    OE->>Order: OrderPaidStockReserveConsumer
    Order->>PG: catalog.stock -= qty (optimistic lock)
    Order->>Cat: StockReserved
```

## Cancel paid order (compensation)

```mermaid
sequenceDiagram
    participant Client
    participant Order as order-service REST
    participant PG as PostgreSQL
    participant Outbox as OutboxPublisher
    participant OE as order.events
    participant PE as payment.events
    participant Cat as catalog.events

    Client->>Order: POST /orders/{id}/cancel
    Order->>PG: order CANCELLED + outbox OrderCancelled
    Outbox->>OE: OrderCancelled
    OE->>Order: OrderCancelledStockReleaseConsumer
    Order->>PG: catalog.stock += qty
    Order->>Cat: StockReleased
    OE->>Order: OrderCancelledRefundConsumer
    Order->>PE: PaymentRefundRequested
    PE->>Order: MockPaymentRefundConsumer
    Order->>PE: PaymentRefunded
```

## Rules

| Previous status | Stock on cancel | Payment compensation |
|-----------------|-----------------|----------------------|
| `PENDING_PAYMENT` | no release (stock not reserved yet) | none |
| `PAID` | `StockReleased` on `catalog.events` | `PaymentRefundRequested` → mock `PaymentRefunded` |

Stock reservation happens on **`OrderPaid`**, not on `OrderCreated`, so unpaid orders do not hold inventory.

Idempotency: every consumer records `(consumer_name, event_id)` in `ordering.processed_events`.

Domain outbox events (`OrderCreated`, `OrderPaid`, `OrderCancelled`) are written in the **same transaction** as the order row update.
