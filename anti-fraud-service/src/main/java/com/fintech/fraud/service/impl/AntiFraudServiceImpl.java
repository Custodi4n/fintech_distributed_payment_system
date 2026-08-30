package com.fintech.fraud.service.impl;

import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.common.events.PaymentCreatedEvent;
import com.fintech.fraud.domain.BlacklistedAccount;
import com.fintech.fraud.domain.FraudCheckAudit;
import com.fintech.fraud.repository.BlacklistedAccountRepository;
import com.fintech.fraud.repository.FraudCheckAuditRepository;
import com.fintech.fraud.service.AntiFraudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntiFraudServiceImpl implements AntiFraudService {

    private final BlacklistedAccountRepository blacklistRepository;
    private final FraudCheckAuditRepository auditRepository;

    @Value("${app.fraud.max-transaction-amount:100000.00}")
    private BigDecimal maxTransactionAmount;

    @Override
    @Transactional
    public FraudCheckVerdictEvent evaluatePayment(PaymentCreatedEvent event) {
        log.info("Evaluating payment {} for fraud risk. Amount: {}", event.getPaymentId(), event.getAmount());

        boolean approved = true;
        String rejectReason = null;

        // Проверка 1: Черный список
        if (blacklistRepository.existsByAccountId(event.getSenderAccountId())) {
            approved = false;
            rejectReason = "Sender account is in the fraud blacklist";
            log.warn("Payment {} REJECTED: Sender account {} is blacklisted", event.getPaymentId(), event.getSenderAccountId());
        }
        // Проверка 2: Превышение лимита разовой суммы
        else if (event.getAmount().compareTo(maxTransactionAmount) > 0) {
            approved = false;
            rejectReason = "Transaction amount exceeds single limit of " + maxTransactionAmount;
            log.warn("Payment {} REJECTED: Amount {} exceeds limit", event.getPaymentId(), event.getAmount());
        } else {
            log.info("Payment {} APPROVED by Anti-Fraud rules", event.getPaymentId());
        }

        // Сохраняем аудит в БД
        FraudCheckAudit audit = FraudCheckAudit.builder()
                .paymentId(event.getPaymentId())
                .senderAccountId(event.getSenderAccountId())
                .amount(event.getAmount())
                .approved(approved)
                .reason(rejectReason)
                .build();
        auditRepository.save(audit);

        return FraudCheckVerdictEvent.builder()
                .paymentId(event.getPaymentId())
                .approved(approved)
                .reason(rejectReason)
                .evaluatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public void addToBlacklist(UUID accountId, String reason) {
        log.info("Adding account {} to fraud blacklist. Reason: {}", accountId, reason);
        if (!blacklistRepository.existsByAccountId(accountId)) {
            blacklistRepository.save(BlacklistedAccount.builder()
                    .accountId(accountId)
                    .reason(reason)
                    .build());
        }
    }
}