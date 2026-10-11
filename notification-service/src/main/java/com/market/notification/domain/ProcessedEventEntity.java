package com.market.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "notification", name = "processed_events")
@IdClass(ProcessedEventEntity.ProcessedEventId.class)
@Getter
@Setter
public class ProcessedEventEntity {

    @Id
    @Column(name = "consumer_name", length = 128)
    private String consumerName;

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class ProcessedEventId implements Serializable {
        private String consumerName;
        private UUID eventId;
    }
}
