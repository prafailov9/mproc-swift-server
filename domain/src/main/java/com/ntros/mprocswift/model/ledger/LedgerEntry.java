package com.ntros.mprocswift.model.ledger;

import com.ntros.mprocswift.model.transactions.Transaction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Data
@RequiredArgsConstructor
@Table(name = "ledger_entries")
public class LedgerEntry {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Integer ledgerEntryId;

  @Column(name = "entry_group_key", nullable = false)
  private String entryGroupKey;

  @Column(name = "entry_seq", nullable = false)
  private Integer entrySequence;

  @ManyToOne
  @JoinColumn(name = "transaction_id")
  private Transaction transaction;

  @ManyToOne
  @JoinColumn(name = "ledger_account_id")
  private LedgerAccount ledgerAccount;

  private long amount;

  private OffsetDateTime entryDate;
  private String description;

  @Override
  public String toString() {
    return "LedgerEntry{"
        + "ledgerEntryId="
        + ledgerEntryId
        + ", entryGroupKey='"
        + entryGroupKey
        + '\''
        + ", txnId="
        + transaction.getTransactionId()
        + ", ledgerAccount="
        + ledgerAccount.getLedgerAccountName()
        + ", amount="
        + amount
        + ", currency="
        + transaction.getCurrency().getCurrencyCode()
        + ", entryDate="
        + entryDate
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
