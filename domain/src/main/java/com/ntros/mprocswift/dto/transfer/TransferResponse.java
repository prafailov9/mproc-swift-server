package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.dto.quotes.FxQuoteDto;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public abstract class TransferResponse {

  protected IdempotencyStatus status;
  protected String description;
  protected String idemKey;

  private MoneyDto debited;
  private MoneyDto credited;
  private FxQuoteDto fxQuoteDto;
  private String rateUpdatedAt;
  private BigDecimal fees;
  private String processedAt;
  // 1:new execution, 0: idempotent replay
  private boolean fresh;
}
