package com.ntros.mprocswift.dto.ledger;

import lombok.Data;

@Data
public class LedgerAccountDTO {

  // temporary for debug TODO: remove after tests
  private int ledgerAccountId;
  private String ledgerAccountType;
  private String ledgerAccountName;
  private String currencyCode;
  private String ownerAccountNumber;
  private boolean isActive;
  private String balance;
  private String balanceUpdatedAt;
}
