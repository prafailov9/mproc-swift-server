package com.ntros.mprocswift.service.transfer.synch;

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
import com.ntros.mprocswift.service.ledger.Posting;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class W2WTransferService
    extends AbstractTransferService<W2WTransferRequest, W2WTransferResponse> {

  private static final long MIN_ALLOWED_FUNDS = 100; // 1 major

  @Override
  protected TransferPlan planAndLock(W2WTransferRequest req) {
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
    if (!ledgerAccountBalanceService.hasAvailableFunds(sender.getWalletId(), MIN_ALLOWED_FUNDS)) {
      throw new InsufficientFundsException(
          "Insufficient funds. balance=" + sender.getBalance() + " required=" + sentMinor);
    }

    // 5. always fetch the rate, even if currencies are the same
    RatedMoneyMovement ratedMoneyMovement =
        currencyExchangeRateService.convert(
            MoneyConverter.toMinor(req.getAmount(), receiver.getCurrency().getExponent()),
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
    return new TransferPlan(txn, sender, receiver, ratedMoneyMovement);
  }

  @Override
  protected void apply(TransferPlan plan) {
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
    moneyTransfer.setTargetCurrencyCode(txn.getCurrency().getCurrencyCode());

    moneyTransferRepository.save(moneyTransfer);

    List<Posting> postings = buildPostings(txn, sender, receiver, movement);

    ledgerEntryService.createLedgerEntries(txn, postings);
  }

  @Override
  protected W2WTransferResponse buildResponse(W2WTransferRequest request, TransferPlan plan) {
    W2WTransferResponse response = assembleResponse(plan.transaction(), plan.ratedMoneyMovement());
    response.setIdemKey(request.getRequestId());
    response.setFresh(true);
    return response;
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
    res.setStatus("COMPLETED");
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
