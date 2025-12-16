package com.ntros.mprocswift.model.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ledger_account_balance")
@Data
@RequiredArgsConstructor
public class LedgerAccountBalance {

  @Id
  @Column(name = "ledger_account_id")
  private Integer ledgerAccountId;

  @Column(name = "balance_minor", nullable = false)
  private long balanceMinor;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime updatedAt;
}
