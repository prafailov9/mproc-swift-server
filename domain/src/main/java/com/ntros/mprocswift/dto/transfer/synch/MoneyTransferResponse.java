package com.ntros.mprocswift.dto.transfer.synch;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public abstract class MoneyTransferResponse {
  private boolean success;
}
