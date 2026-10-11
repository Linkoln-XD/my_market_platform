package com.market.notification.service;

import com.market.notification.domain.ProcessedEventEntity;
import com.market.notification.repository.ProcessedEventRepository;
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
