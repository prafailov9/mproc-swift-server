package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@RequiredArgsConstructor
public class MoneyTransferDTO {

    private String senderAccountNumber;
    private String receiverAccountNumber;

    private String type;
    private String status;

    private String sourceCurrency;
    private String targetCurrency;

    private BigDecimal amount;
    private BigDecimal fees;

    private OffsetDateTime transactionDate;
    private String description;

}
