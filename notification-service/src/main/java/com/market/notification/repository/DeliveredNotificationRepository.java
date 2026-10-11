package com.market.notification.repository;

import com.market.notification.domain.DeliveredNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveredNotificationRepository extends JpaRepository<DeliveredNotificationEntity, UUID> {

    Page<DeliveredNotificationEntity> findAllByOrderByDeliveredAtDesc(Pageable pageable);

    Optional<DeliveredNotificationEntity> findByEventId(UUID eventId);

    long countByEventId(UUID eventId);
}
