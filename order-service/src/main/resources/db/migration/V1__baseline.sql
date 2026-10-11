-- Baseline schemas for the modular monolith.
-- catalog: products, categories, stock (from step 1).
-- ordering: orders, payments, outbox, processed_events (from step 3).
-- Stock moves between these schemas through catalog.events, not inside one local transaction.
-- See docs/architecture.md.

CREATE SCHEMA IF NOT EXISTS catalog;

CREATE SCHEMA IF NOT EXISTS ordering;
