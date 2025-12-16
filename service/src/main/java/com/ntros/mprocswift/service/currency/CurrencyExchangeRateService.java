package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;
import com.ntros.mprocswift.model.currency.MoneyMovement;

import java.math.BigDecimal;

public interface CurrencyExchangeRateService {

  CurrencyExchangeRate getExchangeRate(final Currency source, final Currency target);

  BigDecimal convert(final BigDecimal amount, final Currency source, final Currency target);

  MoneyMovement convert(final BigDecimal amount, final String source, final String target);
}
