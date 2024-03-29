package com.ntros.mprocswift.dto;

import com.ntros.mprocswift.model.transactions.TransactionType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class TransactionDTO {

    private TransactionType transactionType;
    private String sender;
    private String receiver;
    private BigDecimal amount;
    private BigDecimal fees;
    private String currency;
    private String description;

}
