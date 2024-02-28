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

    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    private String cardProvider;
    private String cardNumber;
    private String expirationDate;
    private String cvv;
    private String pinHash;

    private OffsetDateTime creationDate;

}
