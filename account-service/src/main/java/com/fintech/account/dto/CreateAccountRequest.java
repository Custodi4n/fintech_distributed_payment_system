package com.fintech.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAccountRequest {
    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotBlank(message = "Currency cannot be blank")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. USD, EUR, RUB)")
    private String currency;
}