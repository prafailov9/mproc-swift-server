package com.ntros.mprocswift.model.currency;

public record MoneyMovement(Money sentMoney, Money receivedMoney) {
  public MoneyMovement(long sent, Currency sentCurrency, long received, Currency receivedCurrency) {
    this(new Money(sent, sentCurrency), new Money(received, receivedCurrency));
  }
}
