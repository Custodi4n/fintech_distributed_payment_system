package com.fintech.payment.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.common.events.PaymentCreatedEvent;
import com.fintech.payment.domain.OutboxEvent;
import com.fintech.payment.domain.OutboxStatus;
import com.fintech.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.payment-created:payment-created-events}")
    private String paymentCreatedTopic;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending Outbox events to publish to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                if ("PAYMENT_CREATED".equals(event.getEventType())) {
                    PaymentCreatedEvent payload = objectMapper.readValue(event.getPayload(), PaymentCreatedEvent.class);
                    kafkaTemplate.send(paymentCreatedTopic, event.getAggregateId(), payload);
                }

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                log.info("Successfully published Outbox event {} to topic {}", event.getId(), paymentCreatedTopic);
            } catch (Exception e) {
                log.error("Failed to publish Outbox event {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxStatus.FAILED);
            }
            outboxEventRepository.save(event);
        }
    }
}