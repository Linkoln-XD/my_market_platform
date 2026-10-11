package com.market.order.messaging;

import com.market.order.messaging.domain.ProcessedEventEntity;
import com.market.order.messaging.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * @return {@code true} if this consumer has not processed the event yet and the claim was recorded
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessedIfAbsent(String consumerName, UUID eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setConsumerName(consumerName);
        entity.setEventId(eventId);
        try {
            processedEventRepository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
