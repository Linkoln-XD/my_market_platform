CREATE TABLE ordering.orders (
    id             UUID PRIMARY KEY,
    owner_id       VARCHAR(128) NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    currency       CHAR(3)      NOT NULL,
    subtotal_cents BIGINT       NOT NULL CHECK (subtotal_cents >= 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE ordering.order_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID         NOT NULL REFERENCES ordering.orders (id) ON DELETE CASCADE,
    product_id       UUID         NOT NULL,
    sku              VARCHAR(64)  NOT NULL,
    product_name     VARCHAR(256) NOT NULL,
    quantity         INT          NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT       NOT NULL CHECK (unit_price_cents >= 0),
    line_total_cents BIGINT       NOT NULL CHECK (line_total_cents >= 0)
);

CREATE INDEX idx_orders_owner_id ON ordering.orders (owner_id);
CREATE INDEX idx_order_items_order_id ON ordering.order_items (order_id);

-- Idempotency replay for POST /orders (header Idempotency-Key).
CREATE TABLE ordering.idempotency_records (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    owner_id        VARCHAR(128) NOT NULL,
    order_id        UUID         NOT NULL REFERENCES ordering.orders (id),
    response_json   JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_idempotency_expires_at ON ordering.idempotency_records (expires_at);
