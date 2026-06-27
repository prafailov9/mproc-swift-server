package com.ntros.mprocswift.service.transfer;

public interface SyncTransferService<T, R> {

    R transfer(T transferRequest);

}
