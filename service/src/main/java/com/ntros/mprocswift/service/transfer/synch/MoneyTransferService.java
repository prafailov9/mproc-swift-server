package com.ntros.mprocswift.service.transfer.synch;

public interface MoneyTransferService<T, R> {

    R transfer(T transferRequest);

}
