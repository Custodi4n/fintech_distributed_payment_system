package com.fintech.fraud.listener;

import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.common.events.PaymentCreatedEvent;
import com.fintech.fraud.service.AntiFraudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final AntiFraudService antiFraudService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.fraud-verdict:fraud-verdict-events}")
    private String fraudVerdictTopic;

    @KafkaListener(
        topics = "${app.kafka.topics.payment-created:payment-created-events}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        log.info("Received PaymentCreatedEvent from Kafka for paymentId: {}", event.getPaymentId());
        
        // 1. Вычисляем вердикт
        FraudCheckVerdictEvent verdict = antiFraudService.evaluatePayment(event);

        // 2. Отправляем вердикт в Kafka
        kafkaTemplate.send(fraudVerdictTopic, verdict.getPaymentId().toString(), verdict);
        log.info("Published FraudCheckVerdictEvent to Kafka topic '{}' for paymentId: {}", 
                fraudVerdictTopic, verdict.getPaymentId());
    }
}