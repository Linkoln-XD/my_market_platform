package com.market.order.order.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Serializes concurrent work on the same business key within one database transaction.
 * Used so parallel {@code Idempotency-Key} requests create at most one order.
 */
@Component
@RequiredArgsConstructor
public class TransactionAdvisoryLock {

    private final EntityManager entityManager;

    public void lock(String scope, String key) {
        long lockId = Integer.toUnsignedLong((scope + ":" + key).hashCode());
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:lockId)")
                .setParameter("lockId", lockId)
                .getSingleResult();
    }
}
