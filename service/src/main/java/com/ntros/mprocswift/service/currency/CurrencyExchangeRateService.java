package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.model.currency.*;

import java.math.BigDecimal;
import java.util.List;

public interface CurrencyExchangeRateService {

  CurrencyExchangeRate getExchangeRate(final Currency source, final Currency target);

  ConvertedAmount convert(long amount, final Currency source, final Currency target);

  RatedMoneyMovement convert(long amount, final String source, final String target);
}
