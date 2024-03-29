package com.ntros.mprocswift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class CurrencyDTO {

    @NotNull(message = "currency code cannot be null.")
    @NotBlank(message = "currency code cannot be empty.")
    private String currencyCode;

    @NotNull(message = "currency name cannot be null.")
    @NotBlank(message = "currency name cannot be empty.")
    private String currencyName;

    private BigDecimal exchangeRate;
    private Integer scalingFactor;
    private boolean isActive;

}
