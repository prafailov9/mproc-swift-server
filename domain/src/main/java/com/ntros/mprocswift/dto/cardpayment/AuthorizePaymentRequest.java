package com.ntros.mprocswift.dto.cardpayment;

import com.ntros.mprocswift.dto.CardDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuthorizePaymentRequest {

  @NotBlank(message = "Must have currency.")
  private String cardIdentifier;

  private CardDTO cardDTO;

  @Size(min = 5, message = "Amount cannot be less than 5.")
  private BigDecimal amount;

  @NotBlank(message = "Must have currency.")
  private String currency;

  @NotBlank(message = "Must have merchant.")
  private String merchant;
}
