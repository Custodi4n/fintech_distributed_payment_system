package com.fintech.account.service;

import com.fintech.account.dto.AccountResponse;
import com.fintech.account.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);
    AccountResponse getAccountById(UUID accountId);
    List<AccountResponse> getAccountsByUserId(UUID userId);
    AccountResponse deposit(UUID accountId, BigDecimal amount);
    AccountResponse withdraw(UUID accountId, BigDecimal amount);
}