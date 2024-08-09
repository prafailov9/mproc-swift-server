package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.dto.transfer.InternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.InternalTransferResponse;
import com.ntros.mprocswift.service.transfer.InternalTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/transfer/internal")
public class InternalTransferController extends AbstractTransferController<InternalTransferRequest, InternalTransferResponse> {

    protected InternalTransferController(InternalTransferService transferService) {
        super(transferService);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> transfer(@RequestBody @Validated InternalTransferRequest transferRequest) {
        return processTransfer(transferRequest);
    }
}
