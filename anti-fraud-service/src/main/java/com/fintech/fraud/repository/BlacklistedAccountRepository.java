package com.fintech.fraud.repository;

import com.fintech.fraud.domain.BlacklistedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccount, UUID> {
    boolean existsByAccountId(UUID accountId);
}