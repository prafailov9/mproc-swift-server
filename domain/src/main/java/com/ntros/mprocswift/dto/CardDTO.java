package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Data
@RequiredArgsConstructor
public class CardDTO {

    private String type;
    private String cardHolder;
    private String cardProvider;
    private String cardNumberHash;
    private String cvvHash;
    private String pinHash;

    private String expirationDate;

}
