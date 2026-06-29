package com.ntros.mprocswift.model.ledger;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ledger_account_balances")
@Getter
@Setter
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
