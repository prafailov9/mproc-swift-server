package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.converter.FxLegConverter;
import com.ntros.mprocswift.converter.FxQuoteConverter;
import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.dto.quotes.FxQuoteDto;
import com.ntros.mprocswift.dto.transfer.TransferRequest;
import com.ntros.mprocswift.dto.transfer.synch.MoneyTransferResponse;
import com.ntros.mprocswift.exceptions.*;
import com.ntros.mprocswift.exceptions.TransferProcessingFailedException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionQuote;
import com.ntros.mprocswift.model.currency.*;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.transactions.*;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.service.currency.audit.FxLegService;
import com.ntros.mprocswift.service.currency.audit.FxQuoteService;
import com.ntros.mprocswift.service.currency.exchangerate.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.currency.exchangerate.FxRateConversionService;
import com.ntros.mprocswift.service.idempotency.IdempotencyKeyMarkingService;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.ledger.Posting;
import com.ntros.mprocswift.service.wallet.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.COMPLETED;

@Slf4j
public abstract class AbstractTransferService<T extends TransferRequest>
    implements SyncTransferService<T> {

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
  protected final FxQuoteService fxQuoteService;
  protected final FxLegService fxLegService;
  protected final FxRateConversionService fxRateConversionService;
  protected final FxQuoteConverter fxQuoteConverter;
  protected final FxLegConverter fxLegConverter;
  // transaction configs
  protected final PlatformTransactionManager platformTransactionManager;
  private final TransactionTemplate transactionTemplate;
  private static final long MIN_ALLOWED_FUNDS = 100; // 1 major

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
      IdempotencyKeyMarkingService idempotencyKeyService,
      FxQuoteService fxQuoteService,
      FxLegService fxLegService,
      FxRateConversionService fxRateConversionService,
      FxQuoteConverter fxQuoteConverter,
      FxLegConverter fxLegConverter) {
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
    this.fxQuoteService = fxQuoteService;
    this.fxLegService = fxLegService;
    this.fxRateConversionService = fxRateConversionService;
    this.fxQuoteConverter = fxQuoteConverter;
    this.fxLegConverter = fxLegConverter;
  }

  /**
   * Processes a money transfer transaction with idempotency check.
   *
   * @param request - money transfer request
   * @return - processed response
   */
  public MoneyTransferResponse transfer(T request) {
    var key = request.getRequestId();
    var hash = sha256Hex(request);

    // attempt to save the key
    if (!idempotencyKeyService.tryClaim(key, hash)) {
      // save failed -> request already processed. Check hash.
      var existingKey = idempotencyKeyService.load(key);
      if (!existingKey.getRequestHash().equals(hash)) {
        throw new IdempotencyKeyConflictException(
            "Idempotency-Key " + key + " was already used with a different request payload.");
      }

      return replayTransferResponse(idempotencyKeyService.load(key));
    }
    // execute the transfer in its own transaction
    try {
      return transactionTemplate.execute(
          status -> {
            TransferState transferState = createTransferState(request);
            executeTransfer(transferState);
            var response = buildTransferResponse(request, transferState);
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
   * Resolves request type and produces concat str of request values in the format:
   * var1_var2_..._varN;
   */
  protected abstract String requestToStr(T request);

  protected abstract LockedWalletGroup getWalletGroup(T request);

  protected abstract TxnType getTxnType();

  protected TransferState createTransferState(T request) {
    LockedWalletGroup walletGroup = getWalletGroup(request);

    var sender = walletGroup.sender;
    var receiver = walletGroup.receiver;
    long sentMinor =
        MoneyConverter.toMinor(request.getAmount(), sender.getCurrency().getExponent());

    if (sentMinor <= 0) {
      throw new IllegalArgumentException("Transfer amount must be > 0.");
    }

    // 4. check balance
    if (!ledgerAccountBalanceService.hasAvailableFunds(
        sender.getWalletId(), sentMinor + MIN_ALLOWED_FUNDS)) {
      throw new InsufficientFundsException(
          "Insufficient funds. balance=" + sender.getBalance() + " required=" + sentMinor);
    }

    // 5. apply FX conversion
    ConversionQuote quote =
        fxRateConversionService.convert(
            sentMinor,
            sender.getCurrency().getCurrencyCode(),
            receiver.getCurrency().getCurrencyCode());
    // check if requested sent amount matches the calculation
    if (quote.sourceMoney().minorAmount() != sentMinor) {
      log.warn(
          "FX sent mismatch. calculated={}, fxService={}",
          sentMinor,
          quote.sourceMoney().minorAmount());
    }

    // 6. build tx row (store amount in sender currency minor units)
    Transaction txn =
        buildTransaction(
            sender, quote.sourceMoney().minorAmount(), request.getDescription(), getTxnType());

    // 7. build transfer plan
    return new TransferState(txn, sender, receiver, quote);
  }

  private void executeTransfer(TransferState transferState) {
    Transaction txn = transferState.transaction();
    ConversionQuote quote = transferState.conversionQuote();
    var receiver = transferState.receiverWallet();
    var sender = transferState.senderWallet();

    transactionRepository.saveAndFlush(txn);
    MoneyTransfer moneyTransfer = new MoneyTransfer();
    moneyTransfer.setTransactionId(txn.getTransactionId());
    moneyTransfer.setTransaction(txn);
    moneyTransfer.setSenderAccount(sender.getAccount());
    moneyTransfer.setReceiverAccount(receiver.getAccount());
    moneyTransfer.setReceivedAmount(quote.targetMoney().minorAmount());
    moneyTransfer.setTargetCurrencyCode(receiver.getCurrency().getCurrencyCode());

    moneyTransferRepository.save(moneyTransfer);

    FxQuote fxQuote = fxQuoteConverter.toModel(quote);
    List<FxLeg> fxLegs =
        quote.legs().stream()
            .map(
                leg -> {
                  var model = fxLegConverter.toModel(leg);
                  model.setFxQuote(fxQuote);
                  return model;
                })
            .toList();
    fxQuote.setTransaction(txn);
    fxQuoteService.createFxQuote(fxQuote);

    fxLegService.createAllFxLegs(fxLegs);
    fxQuote.setLegs(fxLegs);

    List<Posting> postings = buildPostings(txn, sender, receiver, quote);

    ledgerEntryService.createLedgerEntries(txn, postings);
  }

  protected MoneyTransferResponse buildTransferResponse(T request, TransferState plan) {
    MoneyTransferResponse response = assembleResponse(plan.transaction(), plan.conversionQuote());
    response.setIdemKey(request.getRequestId());
    response.setFresh(true);
    response.setDescription(getTxnType() + "transfer successful");
    return response;
  }

  protected MoneyTransferResponse replayTransferResponse(IdempotencyKey idempotencyKey) {
    log.info("replaying response for key:{}", idempotencyKey);
    MoneyTransferResponse replayed;
    switch (idempotencyKey.getStatus()) {
      case COMPLETED -> replayed = completedResponse(idempotencyKey);
      case IN_PROGRESS, FAILED -> replayed = buildUnresolvedResponse(idempotencyKey);
      default ->
          throw new IllegalArgumentException(
              String.format("Idem-Status not exist:%s", idempotencyKey));
    }

    log.info("Replayed response: {}", replayed);
    return replayed;
  }

  private MoneyTransferResponse completedResponse(IdempotencyKey idempotencyKey) {
    Integer txnId = idempotencyKey.getTransactionId();
    var txn = transactionRepository.findById(txnId).orElseThrow();

    var fxQuote = fxQuoteService.getQuoteByTransaction(txn);
    var fxLegs = fxQuote.getLegs();
    var fxQuoteDto = fxQuoteConverter.toDto(fxQuote);
    for (var leg : fxLegs) {
      var dto = fxLegConverter.toDto(leg);
      fxQuoteDto.legs().add(dto);
    }
    var res = assembleResponse(txn, fxQuoteDto);
    res.setDescription(getTxnType() + " transfer successful");
    res.setIdemKey(idempotencyKey.getIdempotencyKey());
    res.setStatus(idempotencyKey.getStatus());
    res.setFresh(false);
    return res;
  }

  private MoneyTransferResponse buildUnresolvedResponse(IdempotencyKey idempotencyKey) {
    MoneyTransferResponse response = new MoneyTransferResponse();
    response.setFresh(false);
    response.setStatus(idempotencyKey.getStatus());
    return response;
  }

  private MoneyTransferResponse assembleResponse(Transaction txn, ConversionQuote quote) {
    var res = new MoneyTransferResponse();

    MoneyDto debited = buildMoneyDto(quote.targetMoney());
    MoneyDto credited = buildMoneyDto(quote.sourceMoney());
    res.setDebited(debited); // sender side
    res.setCredited(credited); // receiver side
    res.setFxQuote(new FxQuoteDto(debited, credited, quote));
    res.setRateUpdatedAt(
        currencyExchangeRateService
            .getUpdateDateForRate(
                quote.legs().getLast().inputCurrency(), quote.legs().getLast().outputCurrency())
            .toString());
    res.setFees(MoneyConverter.toMajor(txn.getFees(), txn.getCurrency().getExponent()));
    res.setProcessedAt(txn.getTransactionDate().toString());
    res.setStatus(COMPLETED);
    return res;
  }

  private MoneyDto buildMoneyDto(Money money) {
    long amountMinor = money.minorAmount();
    Currency currency = money.currency();
    return new MoneyDto(
        MoneyConverter.toMajor(amountMinor, currency.getExponent()).toString(),
        currency.getCurrencyCode());
  }

  /**
   * Double-entry postings. Direct-currency transactions produce 2 entries at minimum,
   * cross-currency: >2
   */
  private List<Posting> buildPostings(
      Transaction tx, Wallet sender, Wallet receiver, ConversionQuote quote) {

    var type = getTxnType();

    String entryGroupKey = type.name() + ":" + tx.getTransactionId();
    LedgerAccount senderAvail = ledgerAccountService.getAvailableForWallet(sender);
    LedgerAccount receiverAvail = ledgerAccountService.getAvailableForWallet(receiver);

    Currency sc = sender.getCurrency();
    Currency rc = receiver.getCurrency();

    if (sc.getCurrencyId().equals(rc.getCurrencyId())) {
      return List.of(
          new Posting(
              receiverAvail,
              senderAvail,
              quote.sourceMoney().minorAmount(),
              type + ": same currency transfer",
              entryGroupKey));
    }

    LedgerAccount senderBridge = ledgerAccountService.getFxBridgeForCurrency(sc);
    LedgerAccount receiverBridge = ledgerAccountService.getFxBridgeForCurrency(rc);

    return List.of(
        new Posting(
            senderBridge,
            senderAvail,
            quote.sourceMoney().minorAmount(),
            type + ": sender -> FX bridge (" + sc.getCurrencyCode() + ")",
            entryGroupKey),
        new Posting(
            receiverAvail,
            receiverBridge,
            quote.targetMoney().minorAmount(),
            type + ": FX bridge -> receiver (" + rc.getCurrencyCode() + ")",
            entryGroupKey));
  }

  protected Wallet getWalletByCurrency(List<Wallet> wallets, String currency) {
    return wallets.stream()
        .filter(w -> w.getCurrency().getCurrencyCode().equals(currency))
        .findFirst()
        .orElseThrow(
            () -> new WalletNotFoundException("Sender wallet not found for currency " + currency));
  }

  protected Transaction buildTransaction(
      Wallet sender, long senderMinorAmount, String description, TxnType txnType) {
    TransactionStatus status =
        transactionStatusRepository
            .findByStatusName("COMPLETED")
            .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));

    TransactionType type =
        transactionTypeRepository
            .findByTypeName(txnType.name())
            .orElseThrow(() -> new NotFoundException("TX Type not found: " + txnType));

    Transaction txn = new Transaction();
    txn.setTransactionDate(OffsetDateTime.now());
    txn.setCurrency(sender.getCurrency()); // tx currency = sender currency
    txn.setFees(0);
    txn.setAmount(senderMinorAmount);
    txn.setType(type);
    txn.setDescription(description != null ? description : txnType + " transfer");
    txn.setStatus(status);
    return txn;
  }

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
   * @param senderWallet - inputCurrency, from which the monetary amount will be withdrawn
   * @param receiverWallet - outputCurrency, receiving the amount
   * @param conversionQuote - holds the inputCurrency-to-outputCurrency movement data with the
   *     applied exchange appliedRate, if any.
   */
  protected record TransferState(
      Transaction transaction,
      Wallet senderWallet,
      Wallet receiverWallet,
      ConversionQuote conversionQuote) {}

  protected record LockedWalletGroup(Wallet sender, Wallet receiver) {}
}
