package com.ntros.mprocswift.model.currency;

/**
 * Money is stored in the smallest currency amount(USD -> cents)
 * Scale is resolved by Currency.minorUnits
 * @param units
 * @param currency
 */
public record Money(long units, Currency currency) {}
