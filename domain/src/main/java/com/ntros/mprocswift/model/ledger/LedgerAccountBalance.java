package com.ntros.mprocswift.model.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ledger_account_balances")
@Data
@RequiredArgsConstructor
public class LedgerAccountBalance {

  @Id private Integer ledgerAccountId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "ledger_account_id")
  private LedgerAccount ledgerAccount;

  @Column(name = "balance_minor", nullable = false)
  private long balanceMinor;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime updatedAt;
}
