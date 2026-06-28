package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.synch.MoneyTransferResponse;

public interface SyncTransferService<T> {

    MoneyTransferResponse transfer(T transferRequest);

}
