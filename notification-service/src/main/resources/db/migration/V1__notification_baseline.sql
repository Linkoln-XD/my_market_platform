CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.delivered_notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     UUID         NOT NULL,
    order_id     UUID         NOT NULL,
    event_type   VARCHAR(128) NOT NULL,
    channel      VARCHAR(32)  NOT NULL,
    message      TEXT         NOT NULL,
    delivered_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_delivered_notifications_event_id UNIQUE (event_id)
);

CREATE INDEX idx_delivered_notifications_order_id ON notification.delivered_notifications (order_id);

CREATE TABLE notification.processed_events (
    consumer_name VARCHAR(128) NOT NULL,
    event_id      UUID         NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
);
