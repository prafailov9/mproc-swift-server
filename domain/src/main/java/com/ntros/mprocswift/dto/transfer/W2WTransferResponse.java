package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.dto.quotes.FxQuoteDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(
    callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.
public class W2WTransferResponse extends TransferResponse {

  private MoneyDto debited;
  private MoneyDto credited;
  private FxQuoteDto fxQuoteDto;
  private String rateUpdatedAt;
  private BigDecimal fees;
  private String processedAt;
  // 1:new execution, 0: idempotent replay
  private boolean fresh;

  public W2WTransferResponse() {}
}
