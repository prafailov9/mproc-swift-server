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
public class LedgerEntry {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Integer ledgerEntryId;

  @Column(name = "entry_group_key", nullable = false)
  private String entryGroupKey;

  @ManyToOne
  @JoinColumn(name = "transaction_id")
  private Transaction transaction;

  @ManyToOne
  @JoinColumn(name = "ledger_account_id")
  private LedgerAccount ledgerAccount;

  private long amount;

  private OffsetDateTime entryDate;
  private String description;
}
