package com.ntros.mprocswift.model.transactions;

import com.ntros.mprocswift.model.currency.Currency;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id")
    private Transaction relatedTransaction;

    @ManyToOne
    @JoinColumn(name = "transaction_type_id")
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "transaction_status_id")
    private TransactionStatus status;

    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;


    private BigDecimal amount;
    private BigDecimal fees;

    private OffsetDateTime transactionDate;
    private String description;

}
