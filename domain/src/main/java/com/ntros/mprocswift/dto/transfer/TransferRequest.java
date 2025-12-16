package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.validation.GreaterThanZeroTransferAmount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
@GreaterThanZeroTransferAmount
public abstract class TransferRequest {

    @NotNull(message = "source AN cannot be null.")
    @NotBlank(message = "source AN cannot be empty.")
    @Pattern(regexp = "\\d+", message = "source AN must be a number.")
    @Size(min = 8, max = 12, message = "Invalid source AN: must be 8 - 12 digits.")
    private String sourceAccountNumber;

  @NotNull(message = "Transfer amount cannot be null.")
  private BigDecimal amount;

    @NotNull(message = "currency code cannot be null.")
    @NotBlank(message = "currency code cannot be empty.")
    private String currencyCode;
    private String description;

}
