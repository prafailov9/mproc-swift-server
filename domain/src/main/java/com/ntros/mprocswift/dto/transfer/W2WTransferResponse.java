package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.dto.ExchangeRateDto;
import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.model.currency.Money;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(
    callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.
public class W2WTransferResponse extends TransferResponse {

  private MoneyDto debited;
  private MoneyDto credited;
  private ExchangeRateDto exchangeRate;
  private String rateUpdatedAt;
  private BigDecimal fees;
  private String processedAt;
  // 1:new execution, 0: idempotent replay
  private boolean fresh;

  public W2WTransferResponse() {

  }

}
