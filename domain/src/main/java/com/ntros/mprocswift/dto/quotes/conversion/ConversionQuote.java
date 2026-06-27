package com.ntros.mprocswift.dto.quotes.conversion;

import com.ntros.mprocswift.model.currency.Money;

import java.math.BigDecimal;
import java.util.List;

public record ConversionQuote(
    Money sourceMoney, Money targetMoney, BigDecimal effectiveRate, List<ConversionLeg> legs) {

  public static ConversionQuote singleLegQuote(
      Money sourceMoney, Money targetMoney, BigDecimal effectiveRate) {

    return new ConversionQuote(
        sourceMoney,
        targetMoney,
        effectiveRate,
        List.of(
            new ConversionLeg(
                sourceMoney.currency(),
                targetMoney.currency(),
                effectiveRate,
                sourceMoney.minorAmount(),
                targetMoney.minorAmount())));
  }
}
