package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.service.ledger.Posting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
  protected BigDecimal performTransfer(
      Wallet sender, Wallet receiver, W2WTransferRequest transferRequest) {
    BigDecimal transferAmount = transferRequest.getAmount();
    sender.decreaseBalance(transferAmount);

    BigDecimal convertedAmount =
        currencyExchangeRateService.convert(
            transferAmount, sender.getCurrency(), receiver.getCurrency());

    receiver.increaseBalance(convertedAmount);
    updateWalletsAndAccounts(sender, receiver);
    return convertedAmount;
  }

  @Override
  protected void createTransferTransaction(
      Wallet sender, Wallet receiver, W2WTransferRequest transferRequest, TxAmounts txAmounts) {
    Transaction transaction = buildTransaction(sender, receiver, txAmounts.sentValue);
    createAndSaveMoneyTransfer(transaction, sender, receiver);
    createLedgerEntries(transaction, sender, receiver, txAmounts);
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
      Wallet sender, Wallet receiver, BigDecimal normalizedTransferAmount) {
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
    tx.setFees(null);
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
      Transaction transaction, Wallet sender, Wallet receiver, TxAmounts txAmounts) {
    // get available ledgers for both wallets
    String entryGroupKey = "W2W:" + transaction.getTransactionId();
    LedgerAccount senderAvailableAccount = ledgerAccountService.getAvailableForWallet(sender);
    LedgerAccount receiverAvailableAccount = ledgerAccountService.getAvailableForWallet(receiver);

    Currency senderCurrency = sender.getCurrency();
    Currency receiverCurrency = receiver.getCurrency();

    LedgerAccount fxBridgeSender = ledgerAccountService.getFxBridgeForCurrency(senderCurrency);
    LedgerAccount fxBridgeReceiver = ledgerAccountService.getFxBridgeForCurrency(receiverCurrency);

    ledgerEntryService.createLedgerEntries(
        transaction,
        List.of(
            new Posting(
                fxBridgeSender,
                senderAvailableAccount,
                txAmounts.sentValue,
                senderCurrency,
                String.format(
                    "W2W:Sender to System_%s_Account transfer", senderCurrency.getCurrencyCode()),
                entryGroupKey),
            new Posting(
                receiverAvailableAccount,
                fxBridgeReceiver,
                txAmounts.receivedValue,
                receiverCurrency,
                String.format(
                    "W2W:System_%s_Account to Receiver transfer",
                    receiverCurrency.getCurrencyCode()),
                entryGroupKey)));
  }
}
