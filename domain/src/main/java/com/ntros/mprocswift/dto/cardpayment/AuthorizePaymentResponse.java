package com.ntros.mprocswift.dto.cardpayment;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthorizePaymentResponse {

    private RequestResultStatus status;
    private String message;

    private String authCode;

    private String merchant;
    private double price;
    private String currency;
    private String accountNumber;

}
