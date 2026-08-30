package com.fintech.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckVerdictEvent {
    private UUID paymentId;
    private boolean approved;
    private String reason;
    private Instant evaluatedAt;
}