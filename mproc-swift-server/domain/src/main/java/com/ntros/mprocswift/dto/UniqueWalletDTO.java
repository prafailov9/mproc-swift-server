package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@ToString
public class UniqueWalletDTO {

    private String accountNumber;
    private String currencyCode;

}
