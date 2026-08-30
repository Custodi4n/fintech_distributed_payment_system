package com.fintech.common.events;

public enum PaymentStatus {
    PENDING,
    FRAUD_APPROVED,
    FRAUD_REJECTED,
    RESERVED,
    COMPLETED,
    FAILED,
    CANCELLED
}