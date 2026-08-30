package com.fintech.payment.listener;

import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudVerdictListener {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${app.kafka.topics.fraud-verdict:fraud-verdict-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleFraudVerdict(FraudCheckVerdictEvent verdict) {
        log.info("Received FraudCheckVerdictEvent from Kafka for paymentId: {}, approved: {}", 
                verdict.getPaymentId(), verdict.isApproved());
        paymentService.processFraudVerdict(verdict);
    }
}