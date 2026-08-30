package com.fintech.fraud.repository;

import com.fintech.fraud.domain.FraudCheckAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudCheckAuditRepository extends JpaRepository<FraudCheckAudit, UUID> {
    Optional<FraudCheckAudit> findByPaymentId(UUID paymentId);
}