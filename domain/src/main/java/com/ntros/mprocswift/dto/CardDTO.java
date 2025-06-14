package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardDTO {

    private String cardIdHash;

    private String type;
    private String cardHolder;
    private String cardProvider;
    private String cardNumber;
    private String cvv;
    private String pin;
    private String status;

    private String expirationDate;

}
