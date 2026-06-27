package com.ntros.mprocswift.service.currency.exchangerate;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.conversion.ConversionQuote;

public interface FxConversionService {

  ConversionQuote convert(long amount, String sourceCurrencyCode, String targetCurrencyCode);

  ConversionQuote convert(long amount, Currency sourceCurrency, Currency targetCurrency);
}
