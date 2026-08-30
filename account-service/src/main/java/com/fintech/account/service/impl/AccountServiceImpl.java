package com.fintech.account.service.impl;

import com.fintech.account.domain.Account;
import com.fintech.account.domain.AccountStatus;
import com.fintech.account.dto.AccountResponse;
import com.fintech.account.dto.CreateAccountRequest;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.InsufficientFundsException;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for user: {}, currency: {}", request.getUserId(), request.getCurrency());
        Account account = Account.builder()
                .userId(request.getUserId())
                .currency(request.getCurrency().toUpperCase())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse deposit(UUID accountId, BigDecimal amount) {
        log.info("Depositing {} to account {}", amount, accountId);
        Account account = findAccountOrThrow(accountId);
        
        account.setBalance(account.getBalance().add(amount));
        Account updated = accountRepository.save(account);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public AccountResponse withdraw(UUID accountId, BigDecimal amount) {
        log.info("Withdrawing {} from account {}", amount, accountId);
        Account account = findAccountOrThrow(accountId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient balance on account " + accountId + ". Available: " + account.getBalance() + ", requested: " + amount
            );
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updated = accountRepository.save(account);
        return mapToResponse(updated);
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}