package com.ntros.mprocswift.model.transactions.card;

import com.ntros.mprocswift.model.Wallet;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class AuthorizedHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer authorizedHoldId;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "transaction_id")
    private CardAuthorization cardAuthorization;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    private BigDecimal holdAmount;
    private OffsetDateTime holdDate;
    private OffsetDateTime expiresAt;
    private Boolean isReleased;
    private OffsetDateTime releasedAt;
}
