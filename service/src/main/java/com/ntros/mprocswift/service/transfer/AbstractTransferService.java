package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.exceptions.TransferProcessingFailedException;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.service.account.AccountService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.wallet.WalletService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.Modifying;
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
    protected TransactionTypeRepository transactionTypeRepository;
    @Autowired
    protected TransactionStatusRepository transactionStatusRepository;
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
                .thenCombineAsync(receiverFuture, (sender, receiver) -> new TransferContext<>(sender, receiver, transferRequest), executor)
                .thenApplyAsync(this::execInTransaction, executor)
                .exceptionally(ex -> {
                    log.error("Failed to process money transfer: {}", ex.getMessage(), ex.getCause());
                    throw new TransferProcessingFailedException(ex.getMessage(), ex.getCause());
                });
    }

    protected abstract CompletableFuture<S> getSender(T transferRequest);

    protected abstract CompletableFuture<S> getReceiver(T transferRequest);

    protected abstract void performTransfer(S sender, S receiver, T transferRequest);

    protected abstract void createTransferTransaction(S sender, S receiver, T transferRequest);

    protected abstract R buildTransferResponse(T transferRequest);

    @Transactional
    private R doTransfer(S sender, S receiver, T transferRequest) {
        performTransfer(sender, receiver, transferRequest);
        createTransferTransaction(sender, receiver, transferRequest);
        return buildTransferResponse(transferRequest);
    }

    @Transactional
    protected R execInTransaction(TransferContext<T, S> ctx) {
        performTransfer(ctx.sender(), ctx.receiver(), ctx.request());
        createTransferTransaction(ctx.sender(), ctx.receiver(), ctx.request());
        return buildTransferResponse(ctx.request());
    }


    protected record TransferContext<T extends TransferRequest, S>(S sender, S receiver, T request) {
    }


}
