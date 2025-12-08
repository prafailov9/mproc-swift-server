package com.ntros.mprocswift.dto.cardpayment;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardDetails {

    private String cardNumber;
    private String cardCvv;
    private String cardExpiryDate;
    private String cardHolder;
    private String cardProvider;

}
