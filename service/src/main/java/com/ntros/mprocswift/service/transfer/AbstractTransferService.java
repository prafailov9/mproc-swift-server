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
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.wallet.WalletService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public abstract class AbstractTransferService<
        T extends TransferRequest, R extends TransferResponse, S>
    implements TransferService<T, R> {

  @Autowired
  @Qualifier("taskExecutor")
  protected Executor executor;

  @Autowired protected AccountService accountService;
  @Autowired protected TransactionRepository transactionRepository;
  @Autowired protected TransactionTypeRepository transactionTypeRepository;
  @Autowired protected TransactionStatusRepository transactionStatusRepository;
  @Autowired protected MoneyTransferRepository moneyTransferRepository;
  @Autowired protected WalletService walletService;
  @Autowired protected CurrencyExchangeRateService currencyExchangeRateService;
  @Autowired protected LedgerAccountService ledgerAccountService;
  @Autowired protected LedgerEntryService ledgerEntryService;

  public CompletableFuture<R> transfer(T transferRequest) {

    CompletableFuture<S> senderFuture = getSender(transferRequest);
    CompletableFuture<S> receiverFuture = getReceiver(transferRequest);

    return senderFuture
        .thenCombineAsync(
            receiverFuture,
            (sender, receiver) -> new TransferContext<>(sender, receiver, transferRequest),
            executor)
        .thenApplyAsync(this::execInTransaction, executor)
        .exceptionally(
            ex -> {
              log.error("Failed to process money transfer: {}", ex.getMessage(), ex.getCause());
              throw new TransferProcessingFailedException(ex.getMessage(), ex.getCause());
            });
  }

  @Transactional
  protected R execInTransaction(TransferContext<T, S> ctx) {
    BigDecimal convertedAmount = performTransfer(ctx.sender(), ctx.receiver(), ctx.request());

    createTransferTransaction(
        ctx.sender(),
        ctx.receiver(),
        ctx.request(),
        new TxAmounts(ctx.request().getAmount(), convertedAmount));
    return buildTransferResponse(ctx.request());
  }

  protected abstract CompletableFuture<S> getSender(T transferRequest);

  protected abstract CompletableFuture<S> getReceiver(T transferRequest);

  protected abstract BigDecimal performTransfer(S sender, S receiver, T transferRequest);

  protected abstract void createTransferTransaction(
      S sender, S receiver, T transferRequest, TxAmounts txAmounts);

  protected abstract R buildTransferResponse(T transferRequest);

  protected record TransferContext<T extends TransferRequest, S>(S sender, S receiver, T request) {}

  protected static final class TxAmounts {
    BigDecimal sentValue;
    BigDecimal receivedValue;

    TxAmounts(BigDecimal sentValue, BigDecimal receivedValue) {
      this.sentValue = sentValue;
      this.receivedValue = receivedValue;
    }
  }
}
