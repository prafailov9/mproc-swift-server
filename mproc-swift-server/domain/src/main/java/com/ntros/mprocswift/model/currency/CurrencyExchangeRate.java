package com.ntros.mprocswift.model.currency;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class CurrencyExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer currencyExchangeRateId;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "source_currency_id")
    private Currency sourceCurrency;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "target_currency_id")
    private Currency targetCurrency;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;
    private OffsetDateTime updatedDate;

}
