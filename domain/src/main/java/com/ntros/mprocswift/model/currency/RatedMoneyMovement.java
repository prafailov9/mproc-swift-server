package com.ntros.mprocswift.model.currency;

public record RatedMoneyMovement(
    MoneyMovement moneyMovement, CurrencyExchangeRate currencyExchangeRate) {}
