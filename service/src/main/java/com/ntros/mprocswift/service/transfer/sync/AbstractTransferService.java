package com.ntros.mprocswift.service.transfer.sync;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.TransferResponse;
import com.ntros.mprocswift.exceptions.TransferProcessingFailedException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.RatedMoneyMovement;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import com.ntros.mprocswift.repository.transaction.*;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.idempotency.IdempotencyKeyMarkingService;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.wallet.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
public abstract class AbstractTransferService<T extends TransferRequest, R extends TransferResponse>
    implements SyncTransferService<T, R> {

  protected final TransactionRepository transactionRepository;
  protected final TransactionTypeRepository transactionTypeRepository;
  protected final TransactionStatusRepository transactionStatusRepository;
  protected final MoneyTransferRepository moneyTransferRepository;
  protected final CurrencyRepository currencyRepository;

  protected final WalletService walletService;
  protected final CurrencyExchangeRateService currencyExchangeRateService;

  protected final LedgerAccountService ledgerAccountService;
  protected final LedgerEntryService ledgerEntryService;
  protected final LedgerAccountBalanceService ledgerAccountBalanceService;
  protected final IdempotencyKeyMarkingService idempotencyKeyService;
  protected final PlatformTransactionManager platformTransactionManager;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public AbstractTransferService(
      PlatformTransactionManager platformTransactionManager,
      TransactionRepository transactionRepository,
      TransactionTypeRepository transactionTypeRepository,
      TransactionStatusRepository transactionStatusRepository,
      MoneyTransferRepository moneyTransferRepository,
      CurrencyRepository currencyRepository,
      WalletService walletService,
      CurrencyExchangeRateService currencyExchangeRateService,
      LedgerAccountService ledgerAccountService,
      LedgerEntryService ledgerEntryService,
      LedgerAccountBalanceService ledgerAccountBalanceService,
      IdempotencyKeyMarkingService idempotencyKeyService) {
    this.platformTransactionManager = platformTransactionManager;
    transactionTemplate = new TransactionTemplate(platformTransactionManager);

    this.transactionRepository = transactionRepository;
    this.transactionTypeRepository = transactionTypeRepository;
    this.transactionStatusRepository = transactionStatusRepository;
    this.moneyTransferRepository = moneyTransferRepository;
    this.currencyRepository = currencyRepository;
    this.walletService = walletService;
    this.currencyExchangeRateService = currencyExchangeRateService;
    this.ledgerAccountService = ledgerAccountService;
    this.ledgerEntryService = ledgerEntryService;
    this.ledgerAccountBalanceService = ledgerAccountBalanceService;
    this.idempotencyKeyService = idempotencyKeyService;
  }

  /**
   * Processes a money transfer transaction with idempotency check.
   *
   * @param request - money transfer request
   * @return - processed response
   */
  public R transfer(T request) {
    var key = request.getRequestId();
    var hash = sha256Hex(request);

    // attempt to save the key
    if (!idempotencyKeyService.tryClaim(key, hash)) {
      // save failed -> request already processed. Replay it.
      return replayResponse(request, idempotencyKeyService.load(key));
    }
    // execute the transfer in its own transaction
    try {
      return transactionTemplate.execute(
          status -> {
            TransferState transferState = createLockedTransferState(request);
            apply(transferState); // updates + inserts + ledger
            var response = buildResponse(request, transferState);
            idempotencyKeyService.markCompleted(
                request.getRequestId(), transferState.transaction.getTransactionId());
            return response;
          });

    } catch (RuntimeException ex) { // on exception, transaction is rolled back
      idempotencyKeyService.markFailed(key); // REQUIRES_NEW - commits independently of the rollback
      log.error("Error occurred while processing transfer:", ex);
      // will be caught by @ControllerAdvice handler
      throw new TransferProcessingFailedException(ex.getMessage(), ex);
    }
  }

  /**
   * Creates the necessary transfer information for the request. Locks the requestor's wallets to
   * ensure no concurrent call is working with the same data.
   *
   * @param request - the money transfer request
   * @return built transfer state
   */
  protected abstract TransferState createLockedTransferState(T request);

  protected abstract void apply(TransferState plan);

  protected abstract R buildResponse(T request, TransferState plan);

  protected abstract R replayResponse(T request, IdempotencyKey idempotencyKey);

  /**
   * Resolves request type and produces concat str of request values in the format:
   * var1_var2_..._varN;
   */
  protected abstract String requestToStr(T request);

  private String sha256Hex(T request) {
    var input = requestToStr(request);

    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Shared value object, containing information to facilitate a money transfer between sender and
   * receiver.
   *
   * @param transaction - the logical transaction, explaining the money transfer
   * @param senderWallet - source, from which the monetary amount will be withdrawn
   * @param receiverWallet - target, receiving the amount
   * @param ratedMoneyMovement - holds the source-to-target movement data with the applied exchange
   *     rate, if any.
   */
  protected record TransferState(
      Transaction transaction,
      Wallet senderWallet,
      Wallet receiverWallet,
      RatedMoneyMovement ratedMoneyMovement) {}
}
