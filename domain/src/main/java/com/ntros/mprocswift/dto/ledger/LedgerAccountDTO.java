package com.ntros.mprocswift.dto.ledger;

import lombok.Data;

@Data
public class LedgerAccountDTO {

  private String ledgerAccountType;
  private String ledgerAccountName;
  private String currencyCode;
  private String ownerAccountNumber;
  private boolean isActive;
  private String balance;
  private String balanceUpdatedAt;
}
