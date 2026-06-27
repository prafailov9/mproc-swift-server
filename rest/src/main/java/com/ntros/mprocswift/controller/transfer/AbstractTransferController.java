package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.controller.AbstractApiController;
import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.service.transfer.async.AsyncTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
public abstract class AbstractTransferController<
        T extends TransferRequest, R extends TransferResponse>
    extends AbstractApiController {

  protected final AsyncTransferService<T, R> asyncTransferService;

  @Autowired
  @Qualifier("taskExecutor")
  protected Executor executor;

  protected AbstractTransferController(AsyncTransferService<T, R> asyncTransferService) {
    this.asyncTransferService = asyncTransferService;
  }

  protected CompletableFuture<ResponseEntity<?>> processTransferAsync(T transferRequest) {
    return asyncTransferService
        .transferAsync(transferRequest)
        .handleAsync(this::handleResponseAsync, executor);
  }

}
