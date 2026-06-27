package com.ntros.mprocswift.model.currency.conversion;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;

import java.math.BigDecimal;

public record ConversionLeg(
    Currency inputCurrency,
    Currency outputCurrency,
    BigDecimal appliedRate,
    long inputAmount,
    long outputAmount) {

  public static ConversionLeg fromExchangeRate(
      CurrencyExchangeRate currencyExchangeRate, long inputAmount, long outputAmount) {
    return new ConversionLeg(
        currencyExchangeRate.getSourceCurrency(),
        currencyExchangeRate.getTargetCurrency(),
        currencyExchangeRate.getExchangeRate(),
        inputAmount,
        outputAmount);
  }
}
