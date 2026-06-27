package com.ntros.mprocswift.dto.ledger;

import lombok.Data;

@Data
public class LedgerAccountBalanceDto {

  private String ownerId;
  private long balance;
  private String accountCurrency;
  private String type;
  private String updatedAt;
}
