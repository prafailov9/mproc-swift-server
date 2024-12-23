package com.ntros.mprocswift.model.currency;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@RequiredArgsConstructor
@EqualsAndHashCode
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer currencyId;

    @Column(nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private String currencyName;

    private boolean isActive;

}
