package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.exceptions.TransferProcessingFailedException;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.service.account.AccountService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.wallet.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public abstract class AbstractTransferService<T extends TransferRequest, R extends TransferResponse, S> implements TransferService<T, R> {


    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;

    @Autowired
    protected AccountService accountService;
    @Autowired
    protected TransactionRepository transactionRepository;
    @Autowired
    protected MoneyTransferRepository moneyTransferRepository;
    @Autowired
    protected WalletService walletService;
    @Autowired
    protected CurrencyExchangeRateService currencyExchangeRateService;

    public CompletableFuture<R> transfer(T transferRequest) {
        CompletableFuture<S> senderFuture = getSender(transferRequest);
        CompletableFuture<S> receiverFuture = getReceiver(transferRequest);

        return senderFuture
                .thenCombineAsync(receiverFuture, (sender, receiver) ->
                        performTransfer(sender, receiver, transferRequest)
                                .thenComposeAsync(v -> createTransferTransaction(sender, receiver, transferRequest), executor)
                                .thenComposeAsync(v -> buildTransferResponse(transferRequest)), executor)
                .thenComposeAsync(response -> response)
                .exceptionally(ex -> {
                    log.error("Failed to process money transfer: {}", ex.getMessage(), ex.getCause());
                    throw new TransferProcessingFailedException(ex.getMessage(), ex.getCause());
                });
    }

    protected abstract CompletableFuture<S> getSender(T transferRequest);

    protected abstract CompletableFuture<S> getReceiver(T transferRequest);

    protected abstract CompletableFuture<Void> performTransfer(S sender, S receiver, T transferRequest);

    protected abstract CompletableFuture<Void> createTransferTransaction(S sender, S receiver, T transferRequest);

    protected abstract CompletableFuture<R> buildTransferResponse(T transferRequest);

}
