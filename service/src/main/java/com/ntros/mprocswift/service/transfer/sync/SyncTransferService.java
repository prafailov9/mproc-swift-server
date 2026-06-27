package com.ntros.mprocswift.service.transfer.synch;

public interface SyncTransferService<T, R> {

    R transfer(T transferRequest);

}
