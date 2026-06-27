package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public abstract class TransferResponse {

  protected IdempotencyStatus status;
  protected String desc;
  protected String idemKey;
}
