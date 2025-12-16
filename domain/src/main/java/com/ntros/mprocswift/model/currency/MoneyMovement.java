package com.ntros.mprocswift.model.currency;

public record MoneyMovement(Money sentMoney, Money receivedMoney) {
  public MoneyMovement(long sent, Currency sentCur, long recv, Currency recvCur) {
    this(new Money(sent, sentCur), new Money(recv, recvCur));
  }
}
