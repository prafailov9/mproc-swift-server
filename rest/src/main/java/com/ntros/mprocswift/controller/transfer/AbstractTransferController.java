package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.controller.AbstractApiController;
import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.service.transfer.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
public abstract class AbstractTransferController<T extends TransferRequest, R extends TransferResponse> extends AbstractApiController {

    protected final TransferService<T, R> transferService;

    @Autowired
    @Qualifier("taskExecutor")
    protected  Executor executor;

    protected AbstractTransferController(TransferService<T, R> transferService) {
        this.transferService = transferService;
    }

    protected CompletableFuture<ResponseEntity<?>> processTransfer(T transferRequest) {
        return transferService.transfer(transferRequest)
                .handleAsync((this::handleResponseAsync), executor);
    }
}

