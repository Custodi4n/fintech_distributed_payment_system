package com.fintech.account.dto;

import com.fintech.account.domain.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private UUID userId;
    private String currency;
    private BigDecimal balance;
    private AccountStatus status;
    private Instant createdAt;
}