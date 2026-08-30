package com.fintech.fraud.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_check_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "sender_account_id", nullable = false)
    private UUID senderAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;
}