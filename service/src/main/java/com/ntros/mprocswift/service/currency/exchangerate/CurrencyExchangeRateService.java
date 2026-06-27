package com.ntros.mprocswift.service.currency.exchangerate;

import com.ntros.mprocswift.model.currency.*;

import java.time.OffsetDateTime;

public interface CurrencyExchangeRateService {

  CurrencyExchangeRate getExchangeRate(final Currency source, final Currency target);
  OffsetDateTime getUpdateDateForRate(final Currency source, final Currency target);

}
