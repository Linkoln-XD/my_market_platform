CREATE TABLE catalog.categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL,
    slug        VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE catalog.products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  UUID         NOT NULL REFERENCES catalog.categories (id),
    sku          VARCHAR(64)  NOT NULL,
    name         VARCHAR(256) NOT NULL,
    description  TEXT,
    price_cents  BIGINT       NOT NULL CHECK (price_cents >= 0),
    currency     CHAR(3)      NOT NULL DEFAULT 'USD',
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE TABLE catalog.stock (
    product_id UUID PRIMARY KEY REFERENCES catalog.products (id) ON DELETE CASCADE,
    quantity   INT    NOT NULL CHECK (quantity >= 0),
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_products_category_id ON catalog.products (category_id);
CREATE INDEX idx_products_active ON catalog.products (active);
