package com.ntros.mprocswift.model.card;

import com.ntros.mprocswift.model.account.Account;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cardId;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "card_type_id")
    private CardType cardType;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CardStatus status;

    private String cardIdHash;
    private String cardProvider;
    private String cardNumber;
    private String expirationDate;
    private String cvv;
    private String pin;

    private OffsetDateTime creationDate;

}
