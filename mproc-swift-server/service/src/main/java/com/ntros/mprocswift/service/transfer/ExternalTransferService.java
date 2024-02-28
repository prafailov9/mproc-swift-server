package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.ExternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.ExternalTransferResponse;
import com.ntros.mprocswift.model.account.Account;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class ExternalTransferService extends AbstractTransferService<ExternalTransferRequest, ExternalTransferResponse, Account> {

    @Override
    protected CompletableFuture<Account> getSender(ExternalTransferRequest transferRequest) {
        return null;
    }

    @Override
    protected CompletableFuture<Account> getReceiver(ExternalTransferRequest transferRequest) {
        return null;
    }

    @Override
    protected void performTransfer(Account sender, Account receiver, ExternalTransferRequest transferRequest) {

    }

    @Override
    protected void createAndSaveTransaction(Account sender, Account receiver, ExternalTransferRequest transferRequest) {

    }

    @Override
    protected ExternalTransferResponse buildTransferResponse(ExternalTransferRequest transferRequest) {
        return null;
    }

}
