package com.market.order.outbox;

import com.market.order.outbox.domain.OutboxEventEntity;
import com.market.order.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties outboxProperties;

    @Scheduled(fixedDelayString = "${market.outbox.poll-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> batch = outboxEventRepository.lockNextBatch(outboxProperties.batchSize());
        for (OutboxEventEntity row : batch) {
            try {
                kafkaTemplate.send(row.getTopic(), row.getAggregateId().toString(), row.getPayload())
                        .get(15, TimeUnit.SECONDS);
                row.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}", row.getEventId(), ex);
            }
        }
    }
}
