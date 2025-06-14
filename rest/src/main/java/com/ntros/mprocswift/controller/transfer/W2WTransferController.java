package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.service.transfer.W2WTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/transfer/to-wallet")
public class W2WTransferController extends AbstractTransferController<W2WTransferRequest, W2WTransferResponse> {

    protected W2WTransferController(W2WTransferService transferService) {
        super(transferService);
    }

   @PostMapping
   public CompletableFuture<ResponseEntity<?>> transfer(@RequestBody @Validated W2WTransferRequest transferRequest) {
       return processTransfer(transferRequest);
   }

}
