/**
 * Kafka event contracts shared by every producer and consumer.
 *
 * <p>Payload types arrive in step 4. Every domain event carries {@code eventId}
 * (UUID) and {@code occurredAt}. The wire format is JSON.
 */
package com.market.events;
