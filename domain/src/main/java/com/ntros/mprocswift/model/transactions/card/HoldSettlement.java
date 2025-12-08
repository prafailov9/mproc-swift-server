package com.ntros.mprocswift.model.transactions.card;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.transactions.Transaction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class HoldSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_settlement_id")
    private Integer holdSettlementId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "card_authorization_id", nullable = false)
    private CardAuthorization cardAuthorization;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    private BigDecimal settledAmount;
    private OffsetDateTime settledAt;
}


