package com.fintech.payment.service;

import com.fintech.common.events.FraudCheckVerdictEvent;
import com.fintech.payment.dto.CreatePaymentRequest;
import com.fintech.payment.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse initiatePayment(CreatePaymentRequest request);
    PaymentResponse getPaymentById(UUID paymentId);
    void processFraudVerdict(FraudCheckVerdictEvent verdict);
}