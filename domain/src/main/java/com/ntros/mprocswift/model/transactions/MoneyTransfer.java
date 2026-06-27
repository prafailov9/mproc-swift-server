package com.ntros.mprocswift.model.transactions;

import com.ntros.mprocswift.model.account.Account;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Extension of the Transaction entity, holds data specific to money transfers
 */
@Entity
@Data
@RequiredArgsConstructor
@Table(name = "money_transfers")
public class MoneyTransfer {

    // the Primary key is a foreign key to the transaction table.
    @Id
    private Integer transactionId;

    @OneToOne(cascade = CascadeType.ALL) // have to explicitly set cascade type
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;

    @ManyToOne
    @JoinColumn(name = "receiver_account_id")
    private Account receiverAccount;

    @Column(name = "received_amount", nullable = false)
    private long receivedAmount;

    private String targetCurrencyCode;

}
