package com.ntros.mprocswift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class WalletDTO {

    @NotNull(message = "currency code cannot be null.")
    @NotBlank(message = "currency code cannot be empty.")
    private String currencyCode;

    @Pattern(regexp = "\\d+", message = "AN must be a number.")
    @Size(min = 8, max = 12, message = "Invalid AN: must be 8 - 13 digits.")
    @NotNull(message = "AN cannot be null.")
    @NotBlank(message = "AN cannot be empty.")
    private String accountNumber;

    @NotNull(message = "AN cannot be null.")
    private BigDecimal balance;
    private boolean isMain;

}
