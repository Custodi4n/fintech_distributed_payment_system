package com.fintech.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "account-service", url = "${app.services.account-service.url}")
public interface AccountServiceClient {

    @PostMapping("/api/v1/accounts/{accountId}/withdraw")
    Map<String, Object> withdraw(@PathVariable("accountId") UUID accountId, @RequestBody Map<String, BigDecimal> request);

    @PostMapping("/api/v1/accounts/{accountId}/deposit")
    Map<String, Object> deposit(@PathVariable("accountId") UUID accountId, @RequestBody Map<String, BigDecimal> request);
}