package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;

import java.util.concurrent.CompletableFuture;

public interface TransferService<T extends TransferRequest, R extends TransferResponse> {

    CompletableFuture<R> transfer(final T transferRequest);

}
