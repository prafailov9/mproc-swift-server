package com.ntros.mprocswift.model.ledger;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.ExternalAccount;
import com.ntros.mprocswift.model.currency.Currency;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class LedgerAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer ledgerAccountId;

  // read-only generated column to enforce unique owner(wallet, merchant, ext_acc or system account)
  // at db level, not used in appcode
  @Column(name = "owner_key", insertable = false, updatable = false)
  private String ownerKey;

  @ManyToOne(optional = false)
  @JoinColumn(name = "ledger_account_type_id")
  private LedgerAccountType ledgerAccountType;

  @Column(nullable = false)
  private String ledgerAccountName;

  @ManyToOne(optional = false)
  @JoinColumn(name = "currency_id")
  private Currency currency;

  @ManyToOne
  @JoinColumn(name = "wallet_id")
  private Wallet wallet;

  @ManyToOne
  @JoinColumn(name = "merchant_id")
  private Merchant merchant;

  @ManyToOne
  @JoinColumn(name = "external_account_id")
  private ExternalAccount externalAccount;

  @Column(name = "is_active")
  private boolean active;
}
