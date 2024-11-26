package com.ntros.mprocswift.dto.cardpayment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CardPaymentResponse {

    private String status;
    private String message;

    private String merchant;
    private double price;
    private String currency;
    private String accountNumber;

}
