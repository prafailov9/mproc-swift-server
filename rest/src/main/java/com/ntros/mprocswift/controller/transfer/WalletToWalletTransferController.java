package com.ntros.mprocswift.controller.transfer;

import com.ntros.mprocswift.dto.transfer.WalletToWalletTransferRequest;
import com.ntros.mprocswift.dto.transfer.WalletToWalletTransferResponse;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.service.transfer.WalletToWalletTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/transfer/to-wallet")
public class WalletToWalletTransferController extends AbstractTransferController<WalletToWalletTransferRequest, WalletToWalletTransferResponse, Wallet> {

    protected WalletToWalletTransferController(WalletToWalletTransferService transferService) {
        super(transferService);
    }

   @PostMapping
   public CompletableFuture<ResponseEntity<?>> transfer(@RequestBody @Validated WalletToWalletTransferRequest transferRequest) {
       return processTransfer(transferRequest);
   }

}
