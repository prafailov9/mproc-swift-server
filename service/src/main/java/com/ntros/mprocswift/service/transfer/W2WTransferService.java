package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.Money;
import com.ntros.mprocswift.model.currency.MoneyConverter;
import com.ntros.mprocswift.model.currency.MoneyMovement;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.service.ledger.Posting;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class W2WTransferService
    extends AbstractTransferService<W2WTransferRequest, W2WTransferResponse, Wallet> {

  @Override
  protected CompletableFuture<Wallet> getSender(W2WTransferRequest transferRequest) {
    return walletService.getWalletByCurrencyCodeAndAccountNumber(
        transferRequest.getCurrencyCode(), transferRequest.getSourceAccountNumber());
  }

  @Override
  protected CompletableFuture<Wallet> getReceiver(W2WTransferRequest transferRequest) {
    return walletService.getWalletByCurrencyCodeAndAccountNumber(
        transferRequest.getToCurrencyCode(), transferRequest.getSourceAccountNumber());
  }

  @Override
  protected MoneyMovement performTransfer(Wallet sender, Wallet receiver, W2WTransferRequest req) {
    if (!sender.getAccount().getAccountId().equals(receiver.getAccount().getAccountId())) {
      throw new IllegalStateException("W2W wallets must belong to the same account.");
    }

    long sentMinor = MoneyConverter.toMinor(req.getAmount(), sender.getCurrency().getMinorUnits());
    if (!sender.hasAvailableBalance(sentMinor)) {
      throw new InsufficientFundsException("Insufficient funds for W2W transfer.");
    }

    // compute receivedMinor
    BigDecimal receivedMajor =
        currencyExchangeRateService.convert(
            req.getAmount(), sender.getCurrency(), receiver.getCurrency());
    long receivedMinor =
        MoneyConverter.toMinor(receivedMajor, receiver.getCurrency().getMinorUnits());

    // update wallets (minor units)
    sender.decreaseBalance(sentMinor);
    receiver.increaseBalance(receivedMinor);

    updateWalletsAndAccounts(sender, receiver);

    return new MoneyMovement(
        sentMinor, sender.getCurrency(), receivedMinor, receiver.getCurrency());
  }

  @Override
  protected void createTransferTransaction(
      Wallet sender,
      Wallet receiver,
      W2WTransferRequest transferRequest,
      MoneyMovement moneyMovement) {
    Transaction transaction =
        buildTransaction(sender, receiver, moneyMovement.sentMoney().minorAmount());
    createAndSaveMoneyTransfer(transaction, sender, receiver);
    createLedgerEntries(transaction, sender, receiver, moneyMovement);
  }

  @Override
  protected W2WTransferResponse buildTransferResponse(W2WTransferRequest transferRequest) {
    W2WTransferResponse response = new W2WTransferResponse();
    response.setTransferRequest(transferRequest);
    response.setStatus("success");
    return response;
  }

  private void updateWalletsAndAccounts(Wallet sender, Wallet receiver) {
    walletService.updateBalance(sender.getWalletId(), sender.getBalance());
    walletService.updateBalance(receiver.getWalletId(), receiver.getBalance());

    accountService.updateTotalBalance(sender.getAccount());
    accountService.updateTotalBalance(receiver.getAccount());
  }

  private Transaction buildTransaction(
      Wallet sender, Wallet receiver, long normalizedTransferAmount) {
    TransactionStatus status =
        transactionStatusRepository
            .findByStatusName("COMPLETED")
            .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));

    TransactionType type =
        transactionTypeRepository
            .findByTypeName("WALLET_TO_WALLET_TRANSFER")
            .orElseThrow(
                () -> new NotFoundException("TX Type not found: WALLET_TO_WALLET_TRANSFER"));

    Transaction tx = new Transaction();
    tx.setTransactionDate(OffsetDateTime.now());
    tx.setCurrency(sender.getCurrency());
    tx.setFees(0);
    tx.setAmount(normalizedTransferAmount);
    tx.setType(type);
    tx.setDescription(
        String.format(
            "Transferred %s from %s to %s.",
            normalizedTransferAmount,
            sender.getCurrency().getCurrencyCode(),
            receiver.getCurrency().getCurrencyCode()));
    tx.setStatus(status);
    return tx;
  }

  private void createAndSaveMoneyTransfer(Transaction transaction, Wallet sender, Wallet receiver) {
    transactionRepository.saveAndFlush(transaction);

    MoneyTransfer moneyTransfer = new MoneyTransfer();
    moneyTransfer.setTransactionId(transaction.getTransactionId());
    moneyTransfer.setTransaction(transaction);
    moneyTransfer.setSenderAccount(sender.getAccount());
    moneyTransfer.setReceiverAccount(receiver.getAccount());
    moneyTransfer.setTargetCurrencyCode(receiver.getCurrency().getCurrencyCode());

    moneyTransferRepository.save(moneyTransfer);
  }

  private void createLedgerEntries(
      Transaction transaction, Wallet sender, Wallet receiver, MoneyMovement moneyMovement) {
    // get available ledgers for both wallets
    String entryGroupKey = "W2W:" + transaction.getTransactionId();
    LedgerAccount senderLedger = ledgerAccountService.getAvailableForWallet(sender);
    LedgerAccount receiverLedger = ledgerAccountService.getAvailableForWallet(receiver);

    Currency senderCurrency = sender.getCurrency();
    Currency receiverCurrency = receiver.getCurrency();

    // single posting for equal currency
    List<Posting> postings = new ArrayList<>();
    if (senderCurrency.getCurrencyId().equals(receiverCurrency.getCurrencyId())) {
      postings.add(
          new Posting(
              receiverLedger,
              senderLedger,
              moneyMovement.sentMoney().minorAmount(),
              String.format(
                  "W2W: Sender %s -> Receiver %s Transfer",
                  sender.getCurrency().getCurrencyCode(), receiver.getCurrency().getCurrencyCode()),
              entryGroupKey));
    } else {
      // for dif currencies, create multiple postings with system bridge accounts. easier to
      // calculate and validate amounts, add fees later
      LedgerAccount senderBridge = ledgerAccountService.getFxBridgeForCurrency(senderCurrency);
      LedgerAccount receiverBridge = ledgerAccountService.getFxBridgeForCurrency(receiverCurrency);
      postings.addAll(
          List.of(
              new Posting(
                  senderBridge,
                  senderLedger,
                  moneyMovement.sentMoney().minorAmount(),
                  String.format(
                      "W2W:Sender to System_%s_Account transfer", senderCurrency.getCurrencyCode()),
                  entryGroupKey),
              new Posting(
                  receiverLedger,
                  receiverBridge,
                  moneyMovement.receivedMoney().minorAmount(),
                  String.format(
                      "W2W:System_%s_Account to Receiver transfer",
                      receiverCurrency.getCurrencyCode()),
                  entryGroupKey)));
    }
    ledgerEntryService.createLedgerEntries(transaction, postings);
  }
}
