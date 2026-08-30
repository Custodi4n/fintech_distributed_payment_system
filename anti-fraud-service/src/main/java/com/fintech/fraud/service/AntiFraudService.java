package com.fintech.fraud.service;

import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.common.events.PaymentCreatedEvent;

import java.util.UUID;

public interface AntiFraudService {
    FraudCheckVerdictEvent evaluatePayment(PaymentCreatedEvent event);
    void addToBlacklist(UUID accountId, String reason);
}