package com.ntros.mprocswift.service.transfer.async;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;

import java.util.concurrent.CompletableFuture;

public interface AsyncTransferService<T extends TransferRequest, R extends TransferResponse> {

    CompletableFuture<R> transferAsync(final T transferRequest);

}
