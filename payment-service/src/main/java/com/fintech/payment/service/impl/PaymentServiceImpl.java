package com.fintech.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.common.events.PaymentCreatedEvent;
import com.fintech.common.events.PaymentStatus;
import com.fintech.payment.client.AccountServiceClient;
import com.fintech.payment.domain.OutboxEvent;
import com.fintech.payment.domain.OutboxStatus;
import com.fintech.payment.domain.Payment;
import com.fintech.payment.dto.CreatePaymentRequest;
import com.fintech.payment.dto.PaymentResponse;
import com.fintech.payment.repository.OutboxEventRepository;
import com.fintech.payment.repository.PaymentRepository;
import com.fintech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @SneakyThrows
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        log.info("Initiating payment from {} to {}, amount: {}", 
                request.getSenderAccountId(), request.getReceiverAccountId(), request.getAmount());

        // 1. Создаем платеж со статусом PENDING
        Payment payment = Payment.builder()
                .senderAccountId(request.getSenderAccountId())
                .receiverAccountId(request.getReceiverAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 2. Transactional Outbox: сохраняем событие в Outbox в той же транзакции БД
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(savedPayment.getId())
                .senderAccountId(savedPayment.getSenderAccountId())
                .receiverAccountId(savedPayment.getReceiverAccountId())
                .amount(savedPayment.getAmount())
                .currency(savedPayment.getCurrency())
                .createdAt(Instant.now())
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(savedPayment.getId().toString())
                .eventType("PAYMENT_CREATED")
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(outboxEvent);
        log.info("Payment {} saved with PENDING status and outbox event created", savedPayment.getId());

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public void processFraudVerdict(FraudCheckVerdictEvent verdict) {
        log.info("Processing fraud verdict for payment {}. Approved: {}", verdict.getPaymentId(), verdict.isApproved());

        Payment payment = paymentRepository.findById(verdict.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + verdict.getPaymentId()));

        // Если антифрод отклонил
        if (!verdict.isApproved()) {
            payment.setStatus(PaymentStatus.FRAUD_REJECTED);
            payment.setFailReason(verdict.getReason());
            paymentRepository.saveAndFlush(payment);
            log.warn("Payment {} was REJECTED by Anti-Fraud: {}", payment.getId(), verdict.getReason());
            return;
        }

        // Если одобрено — выполняем денежный перевод через Account Service
        try {
            payment.setStatus(PaymentStatus.FRAUD_APPROVED);
            
            // 1. Списание у отправителя
            accountServiceClient.withdraw(payment.getSenderAccountId(), Map.of("amount", payment.getAmount()));
            
            // 2. Зачисление получателю
            accountServiceClient.deposit(payment.getReceiverAccountId(), Map.of("amount", payment.getAmount()));

            payment.setStatus(PaymentStatus.COMPLETED);
            log.info("Payment {} successfully COMPLETED!", payment.getId());
        } catch (Exception e) {
            log.error("Failed to execute money transfer for payment {}: {}", payment.getId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            
            // Защита от переполнения строки ошибки
            String rawMessage = e.getMessage() != null ? e.getMessage() : "Unknown transfer error";
            String sanitizedReason = rawMessage.length() > 250 ? rawMessage.substring(0, 250) + "..." : rawMessage;
            
            payment.setFailReason(sanitizedReason);
        }

        paymentRepository.saveAndFlush(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .senderAccountId(payment.getSenderAccountId())
                .receiverAccountId(payment.getReceiverAccountId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .failReason(payment.getFailReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}