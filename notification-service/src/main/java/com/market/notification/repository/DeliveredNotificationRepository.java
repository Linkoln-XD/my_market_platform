package com.market.notification.repository;

import com.market.notification.domain.DeliveredNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveredNotificationRepository extends JpaRepository<DeliveredNotificationEntity, UUID> {

    Optional<DeliveredNotificationEntity> findByEventId(UUID eventId);

    long countByEventId(UUID eventId);
}
