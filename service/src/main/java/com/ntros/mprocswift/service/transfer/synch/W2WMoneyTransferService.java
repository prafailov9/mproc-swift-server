package com.ntros.mprocswift.service.transfer.synch;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.dto.transfer.synch.W2WMoneyTransferRequest;
import com.ntros.mprocswift.dto.transfer.synch.W2WMoneyTransferResponse;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.exceptions.WalletNotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.MoneyConverter;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.service.ledger.Posting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

// @Service
// @Slf4j
// public class W2WMoneyTransferService
//    extends AbstractMoneyTransferService<W2WMoneyTransferRequest, W2WMoneyTransferResponse> {
//  @Override
//  protected TransferPlan planAndLock(W2WTransferRequest req) {
//    // 1) Lock all wallets for this account number (prevents deadlocks + ensures both rows locked)
//    List<Wallet> lockedWallets = walletService.getAllWalletsLocked(req.getSourceAccountNumber());
//
//    // 2) Select sender/receiver wallets by currency
//    Wallet sender =
//        lockedWallets.stream()
//            .filter(w -> w.getCurrency().getCurrencyCode().equals(req.getCurrencyCode()))
//            .findFirst()
//            .orElseThrow(
//                () ->
//                    new WalletNotFoundException(
//                        "Sender wallet not found for currency " + req.getCurrencyCode()));
//
//    Wallet receiver =
//        lockedWallets.stream()
//            .filter(w -> w.getCurrency().getCurrencyCode().equals(req.getToCurrencyCode()))
//            .findFirst()
//            .orElseThrow(
//                () ->
//                    new WalletNotFoundException(
//                        "Receiver wallet not found for currency " + req.getToCurrencyCode()));
//
//    // 3) Convert request major amount -> sender minor units
//    long sentMinor = MoneyConverter.toMinor(req.getAmount(),
// sender.getCurrency().getMinorUnits());
//
//    if (sentMinor <= 0) {
//      throw new IllegalArgumentException("Transfer amount must be > 0.");
//    }
//
//    // 4) Funds check (minor units)
//    if (!sender.hasAvailableBalance(sentMinor)) {
//      throw new InsufficientFundsException(
//          "Insufficient funds. balance=" + sender.getBalance() + " required=" + sentMinor);
//    }
//
//    // 5) FX conversion only if currencies differ
//    MoneyMovement movement;
//    if (sender.getCurrency().getCurrencyId().equals(receiver.getCurrency().getCurrencyId())) {
//      movement =
//          new MoneyMovement(
//              sentMinor, sender.getCurrency(),
//              sentMinor, receiver.getCurrency());
//    } else {
//      // use your FX service; returns MoneyMovement (sent + received)
//      // but your FX service expects BigDecimal amount + codes, so pass req amount.
//      movement =
//          currencyExchangeRateService.convert(
//              req.getAmount(),
//              sender.getCurrency().getCurrencyCode(),
//              receiver.getCurrency().getCurrencyCode());
//
//      // safety: ensure sent side matches what we calculated
//      // (optional, but nice for invariants)
//      if (movement.sentMoney().minorAmount() != sentMinor) {
//        // you can choose to enforce or just log
//        log.warn(
//            "FX sent mismatch. calculated={}, fxService={}",
//            sentMinor,
//            movement.sentMoney().minorAmount());
//      }
//    }
//
//    // 6) Build Transaction row (store amount in sender currency minor units)
//    Transaction tx =
//        buildTransaction(
//            sender, receiver, movement.sentMoney().minorAmount(), req.getDescription());
//
//    // 7) Build postings
//    List<Posting> postings = buildPostings(tx, sender, receiver, movement);
//
//    return new TransferPlan(tx, movement, postings);
//  }
//
//  @Override
//  protected TransferPlan planAndLock(W2WMoneyTransferRequest request) {
//    return null;
//  }
//
//  @Override
//  protected void apply(TransferPlan plan) {
//    Transaction tx = plan.transaction();
//    MoneyMovement movement = plan.moneyMovement();
//
//    // We need the wallets again? We already locked them; easiest is to embed wallet refs in plan.
//    // For now, simplest: rebuild postings and also update balances where you still use wallet
//    // table.
//    // Better: include sender/receiver wallets in TransferPlan; see note below.
//    //
//    // I'll show the “better” version right after this snippet.
//    throw new UnsupportedOperationException("Use the enhanced TransferPlan version shown below.");
//  }
//
//  @Override
//  protected W2WMoneyTransferResponse buildResponse(
//      W2WMoneyTransferRequest request, TransferPlan plan) {
//    return null;
//  }
//
//  private Transaction buildTransaction(
//      Wallet sender, Wallet receiver, long senderMinorAmount, String description) {
//    TransactionStatus status =
//        transactionStatusRepository
//            .findByStatusName("COMPLETED")
//            .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));
//
//    TransactionType type =
//        transactionTypeRepository
//            .findByTypeName("WALLET_TO_WALLET_TRANSFER")
//            .orElseThrow(
//                () -> new NotFoundException("TX Type not found: WALLET_TO_WALLET_TRANSFER"));
//
//    Transaction tx = new Transaction();
//    tx.setTransactionDate(OffsetDateTime.now());
//    tx.setCurrency(sender.getCurrency()); // tx currency = sender currency
//    tx.setFees(0);
//    tx.setAmount(senderMinorAmount);
//    tx.setType(type);
//    tx.setDescription(description != null ? description : "W2W transfer");
//    tx.setStatus(status);
//    return tx;
//  }
//
//  private List<Posting> buildPostings(
//      Transaction tx, Wallet sender, Wallet receiver, MoneyMovement movement) {
//    String entryGroupKey =
//        "W2W:" + (tx.getTransactionId() != null ? tx.getTransactionId() : "PENDING");
//
//    LedgerAccount senderAvail = ledgerAccountService.getAvailableForWallet(sender);
//    LedgerAccount receiverAvail = ledgerAccountService.getAvailableForWallet(receiver);
//
//    Currency sc = sender.getCurrency();
//    Currency rc = receiver.getCurrency();
//
//    if (sc.getCurrencyId().equals(rc.getCurrencyId())) {
//      return List.of(
//          new Posting(
//              receiverAvail,
//              senderAvail,
//              movement.sentMoney().minorAmount(),
//              "W2W: same currency transfer",
//              entryGroupKey));
//    }
//
//    LedgerAccount senderBridge = ledgerAccountService.getFxBridgeForCurrency(sc);
//    LedgerAccount receiverBridge = ledgerAccountService.getFxBridgeForCurrency(rc);
//
//    return List.of(
//        new Posting(
//            senderBridge,
//            senderAvail,
//            movement.sentMoney().minorAmount(),
//            "W2W: sender -> FX bridge (" + sc.getCurrencyCode() + ")",
//            entryGroupKey),
//        new Posting(
//            receiverAvail,
//            receiverBridge,
//            movement.receivedMoney().minorAmount(),
//            "W2W: FX bridge -> receiver (" + rc.getCurrencyCode() + ")",
//            entryGroupKey));
//  }
// }
