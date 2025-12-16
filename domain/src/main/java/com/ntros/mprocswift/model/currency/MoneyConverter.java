package com.ntros.mprocswift.model.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money is stored in the smallest currency amount(USD -> cents) Scale is resolved by
 * Currency.minorAmount
 */
public final class MoneyConverter {

  public static long toMinor(BigDecimal major, int exponent) {
    try {
      BigDecimal normalized = major.setScale(exponent, RoundingMode.UNNECESSARY);
      return normalized.movePointRight(exponent).longValueExact();
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException(
          "Invalid amount scale for currency exponent=" + exponent + ", amount=" + major, ex);
    }
  }

  public static BigDecimal toMajor(long minor, int exponent) {
    return BigDecimal.valueOf(minor, exponent);
  }
}
