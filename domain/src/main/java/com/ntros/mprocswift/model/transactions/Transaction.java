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

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_id")
    private TransactionType type;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status_id")
    private TransactionStatus status;

    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;


    private BigDecimal amount;
    private BigDecimal fees;

    private OffsetDateTime transactionDate;
    private String description;

}
