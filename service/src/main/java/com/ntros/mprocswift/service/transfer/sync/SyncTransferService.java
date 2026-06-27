package com.ntros.mprocswift.service.transfer.sync;

public interface SyncTransferService<T, R> {

    R transfer(T transferRequest);

}
