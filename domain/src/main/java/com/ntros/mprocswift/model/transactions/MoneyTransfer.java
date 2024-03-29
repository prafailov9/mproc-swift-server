package com.ntros.mprocswift.model.transactions;

import com.ntros.mprocswift.model.account.Account;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Extension of the Transaction entity, holds data specific to money transfers
 */
@Entity
@Data
@RequiredArgsConstructor
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

    private String targetCurrencyCode;

}
