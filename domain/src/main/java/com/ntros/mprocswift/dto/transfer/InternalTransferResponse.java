package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.dto.quotes.FxQuoteDto;

import java.math.BigDecimal;

public class InternalTransferResponse extends TransferResponse {

  private MoneyDto debited;
  private MoneyDto credited;
  private FxQuoteDto fxQuoteDto;
  private String rateUpdatedAt;
  private BigDecimal fees;
  private String processedAt;
  // 1:new execution, 0: idempotent replay
  private boolean fresh;

  public InternalTransferResponse() {}
}
