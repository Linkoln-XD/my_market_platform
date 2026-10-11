CREATE TABLE ordering.outbox (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(128) NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    event_id       UUID         NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ,
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_unpublished ON ordering.outbox (created_at) WHERE published_at IS NULL;
