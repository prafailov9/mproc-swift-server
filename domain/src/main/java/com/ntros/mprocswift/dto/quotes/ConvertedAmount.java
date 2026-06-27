package com.ntros.mprocswift.dto.quotes;

import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;

public record ConvertedAmount(long amount, CurrencyExchangeRate appliedRate) {}
