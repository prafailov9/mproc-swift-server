package com.ntros.mprocswift.model.transactions.card;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.transactions.Transaction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class CardAuthorization {
    // the Primary key is a foreign key to the transaction table.
    @Id
    private Integer transactionId;

    @OneToOne(cascade = CascadeType.ALL) // have to explicitly set cascade type
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    private String authorizationCode;
    private OffsetDateTime authorizedAt;
}
