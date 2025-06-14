package com.ntros.mprocswift.dto.cardpayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthorizePaymentRequest {


    @NotBlank(message = "Must have currency.")
    private String cardIdentifier;

    @Size(min = 5, message = "Amount cannot be less than 5.")
    private Double amount;

    @NotBlank(message = "Must have currency.")
    private String currency;

    @NotBlank(message = "Must have merchant.")
    private String merchant;
}
