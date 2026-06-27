package com.ntros.mprocswift.service.transfer.sync;

import com.ntros.mprocswift.dto.ExchangeRateDto;
import com.ntros.mprocswift.dto.MoneyDto;
import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.exceptions.WalletNotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.*;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.transactions.*;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.idempotency.IdempotencyKeyMarkingService;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.ledger.Posting;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.ntros.mprocswift.service.wallet.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.COMPLETED;

@Service
@Slf4j
public class W2WTransferService
    extends AbstractTransferService<W2WTransferRequest, W2WTransferResponse> {

  private static final long MIN_ALLOWED_FUNDS = 100; // 1 major

  public W2WTransferService(
      PlatformTransactionManager tm,
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
    super(
        tm,
        transactionRepository,
        transactionTypeRepository,
        transactionStatusRepository,
        moneyTransferRepository,
        currencyRepository,
        walletService,
        currencyExchangeRateService,
        ledgerAccountService,
        ledgerEntryService,
        ledgerAccountBalanceService,
        idempotencyKeyService);
  }

  @Override
  protected TransferState createLockedTransferState(W2WTransferRequest req) {
    // 1. lock all wallets for this account number, prevents deadlocks + ensures both rows locked
    List<Wallet> lockedWallets = walletService.getAllWalletsLocked(req.getSourceAccountNumber());

    // 2. get sender/receiver by currency
    Wallet sender =
        lockedWallets.stream()
            .filter(w -> w.getCurrency().getCurrencyCode().equals(req.getCurrencyCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new WalletNotFoundException(
                        "Sender wallet not found for currency " + req.getCurrencyCode()));

    Wallet receiver =
        lockedWallets.stream()
            .filter(w -> w.getCurrency().getCurrencyCode().equals(req.getToCurrencyCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new WalletNotFoundException(
                        "Receiver wallet not found for currency " + req.getToCurrencyCode()));

    long sentMinor = MoneyConverter.toMinor(req.getAmount(), sender.getCurrency().getExponent());

    if (sentMinor <= 0) {
      throw new IllegalArgumentException("Transfer amount must be > 0.");
    }

    // 4. check balance
    if (!ledgerAccountBalanceService.hasAvailableFunds(
        sender.getWalletId(), sentMinor + MIN_ALLOWED_FUNDS)) {
      throw new InsufficientFundsException(
          "Insufficient funds. balance=" + sender.getBalance() + " required=" + sentMinor);
    }

    // 5. always fetch the rate, even if currencies are the same
    RatedMoneyMovement ratedMoneyMovement =
        currencyExchangeRateService.convert(
            sentMinor,
            sender.getCurrency().getCurrencyCode(),
            receiver.getCurrency().getCurrencyCode());

    // safety: ensure sent side matches what we calculated
    // (optional, but nice for invariants)
    if (ratedMoneyMovement.moneyMovement().sentMoney().minorAmount() != sentMinor) {
      // you can choose to enforce or just log
      log.warn(
          "FX sent mismatch. calculated={}, fxService={}",
          sentMinor,
          ratedMoneyMovement.moneyMovement().sentMoney().minorAmount());
    }

    // 6. build tx row (store amount in sender currency minor units)
    Transaction txn =
        buildTransaction(
            sender,
            ratedMoneyMovement.moneyMovement().sentMoney().minorAmount(),
            req.getDescription());

    // 7. build transfer plan
    return new TransferState(txn, sender, receiver, ratedMoneyMovement);
  }

  @Override
  protected void apply(TransferState plan) {
    Transaction txn = plan.transaction();
    MoneyMovement movement = plan.ratedMoneyMovement().moneyMovement();
    var receiver = plan.receiverWallet();
    var sender = plan.senderWallet();

    transactionRepository.saveAndFlush(txn);
    MoneyTransfer moneyTransfer = new MoneyTransfer();
    moneyTransfer.setTransactionId(txn.getTransactionId());
    moneyTransfer.setTransaction(txn);
    moneyTransfer.setSenderAccount(sender.getAccount());
    moneyTransfer.setReceiverAccount(receiver.getAccount());
    moneyTransfer.setReceivedAmount(movement.receivedMoney().minorAmount());
    moneyTransfer.setTargetCurrencyCode(receiver.getCurrency().getCurrencyCode());

    moneyTransferRepository.save(moneyTransfer);

    List<Posting> postings = buildPostings(txn, sender, receiver, movement);

    ledgerEntryService.createLedgerEntries(txn, postings);
  }

  @Override
  protected W2WTransferResponse buildResponse(W2WTransferRequest request, TransferState plan) {
    W2WTransferResponse response = assembleResponse(plan.transaction(), plan.ratedMoneyMovement());
    response.setIdemKey(request.getRequestId());
    response.setFresh(true);
    response.setDesc("transfer successful");
    return response;
  }

  @Override
  protected W2WTransferResponse replayResponse(
      W2WTransferRequest request, IdempotencyKey idempotencyKey) {
    log.info("replaying response for key:{}", idempotencyKey);
    W2WTransferResponse replayed;
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

  private W2WTransferResponse completedResponse(IdempotencyKey idempotencyKey) {
    Integer txnId = idempotencyKey.getTransactionId();
    var txn = transactionRepository.findById(txnId).orElseThrow();
    var moneyTransfer = moneyTransferRepository.findById(txnId).orElseThrow();
    Currency senderCurrency =
        currencyRepository.findByCurrencyCode(txn.getCurrency().getCurrencyCode()).orElseThrow();
    Currency receiverCurrency =
        currencyRepository.findByCurrencyCode(moneyTransfer.getTargetCurrencyCode()).orElseThrow();

    // TODO: add fx_quote for full rate conversion trail
    var rate = currencyExchangeRateService.getExchangeRate(senderCurrency, receiverCurrency);
    var mm =
        new MoneyMovement(
            txn.getAmount(), senderCurrency, moneyTransfer.getReceivedAmount(), receiverCurrency);
    var res = assembleResponse(txn, new RatedMoneyMovement(mm, rate));
    res.setDesc("transfer successful");
    res.setIdemKey(idempotencyKey.getIdempotencyKey());
    res.setStatus(idempotencyKey.getStatus());
    res.setFresh(false);
    return res;
  }

  private W2WTransferResponse buildUnresolvedResponse(IdempotencyKey idempotencyKey) {
    W2WTransferResponse response = new W2WTransferResponse();
    response.setFresh(false);
    response.setStatus(idempotencyKey.getStatus());
    return response;
  }

  @Override
  protected String requestToStr(W2WTransferRequest request) {
    return request.getRequestId()
        + "_"
        + request.getSourceAccountNumber()
        + "_"
        + request.getAmount()
        + "_"
        + request.getCurrencyCode()
        + "_"
        + request.getToCurrencyCode()
        + "_"
        + request.getDescription();
  }

  private W2WTransferResponse assembleResponse(
      Transaction txn, RatedMoneyMovement ratedMoneyMovement) {
    var res = new W2WTransferResponse();

    MoneyDto debited = buildMoneyDto(ratedMoneyMovement.moneyMovement().sentMoney());
    MoneyDto credited = buildMoneyDto(ratedMoneyMovement.moneyMovement().receivedMoney());
    res.setDebited(debited); // sender side
    res.setCredited(credited); // receiver side
    res.setExchangeRate(buildExchangeRateDto(ratedMoneyMovement));
    res.setFees(MoneyConverter.toMajor(txn.getFees(), txn.getCurrency().getExponent()));
    res.setProcessedAt(txn.getTransactionDate().toString());
    res.setStatus(COMPLETED);
    return res;
  }

  private ExchangeRateDto buildExchangeRateDto(RatedMoneyMovement ratedMoneyMovement) {
    var exchangeRate = ratedMoneyMovement.currencyExchangeRate();
    BigDecimal appliedRateValue = exchangeRate.getExchangeRate();
    String source = exchangeRate.getSourceCurrency().getCurrencyCode();
    String target = exchangeRate.getTargetCurrency().getCurrencyCode();

    var exchangeRateDto = new ExchangeRateDto();
    exchangeRateDto.setRateValue(appliedRateValue);
    exchangeRateDto.setSource(source);
    exchangeRateDto.setTarget(target);

    return exchangeRateDto;
  }

  private MoneyDto buildMoneyDto(Money money) {
    long amountMinor = money.minorAmount();
    Currency currency = money.currency();
    return new MoneyDto(
        MoneyConverter.toMajor(amountMinor, currency.getExponent()).toString(),
        currency.getCurrencyCode());
  }

  private Transaction buildTransaction(Wallet sender, long senderMinorAmount, String description) {
    TransactionStatus status =
        transactionStatusRepository
            .findByStatusName("COMPLETED")
            .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));

    TransactionType type =
        transactionTypeRepository
            .findByTypeName("W2W")
            .orElseThrow(() -> new NotFoundException("TX Type not found: W2W"));

    Transaction txn = new Transaction();
    txn.setTransactionDate(OffsetDateTime.now());
    txn.setCurrency(sender.getCurrency()); // tx currency = sender currency
    txn.setFees(0);
    txn.setAmount(senderMinorAmount);
    txn.setType(type);
    txn.setDescription(description != null ? description : "W2W transfer");
    txn.setStatus(status);
    return txn;
  }

  /**
   * Double-entry postings. Direct-currency transactions produce 2 entries at minimum,
   * cross-currency: >2
   */
  private List<Posting> buildPostings(
      Transaction tx, Wallet sender, Wallet receiver, MoneyMovement movement) {
    String entryGroupKey = "W2W:" + tx.getTransactionId();
    LedgerAccount senderAvail = ledgerAccountService.getAvailableForWallet(sender);
    LedgerAccount receiverAvail = ledgerAccountService.getAvailableForWallet(receiver);

    Currency sc = sender.getCurrency();
    Currency rc = receiver.getCurrency();

    if (sc.getCurrencyId().equals(rc.getCurrencyId())) {
      return List.of(
          new Posting(
              receiverAvail,
              senderAvail,
              movement.sentMoney().minorAmount(),
              "W2W: same currency transfer",
              entryGroupKey));
    }

    LedgerAccount senderBridge = ledgerAccountService.getFxBridgeForCurrency(sc);
    LedgerAccount receiverBridge = ledgerAccountService.getFxBridgeForCurrency(rc);

    return List.of(
        new Posting(
            senderBridge,
            senderAvail,
            movement.sentMoney().minorAmount(),
            "W2W: sender -> FX bridge (" + sc.getCurrencyCode() + ")",
            entryGroupKey),
        new Posting(
            receiverAvail,
            receiverBridge,
            movement.receivedMoney().minorAmount(),
            "W2W: FX bridge -> receiver (" + rc.getCurrencyCode() + ")",
            entryGroupKey));
  }
}
