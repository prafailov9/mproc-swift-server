package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.InternalTransferRequest;
import com.ntros.mprocswift.dto.transfer.InternalTransferResponse;
import com.ntros.mprocswift.exceptions.*;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class InternalTransferService extends AbstractTransferService<InternalTransferRequest, InternalTransferResponse, Account> {

    @Override
    protected CompletableFuture<Account> getSender(InternalTransferRequest transferRequest) {
        return accountService.getAccountByAccountNumber(transferRequest.getSourceAccountNumber());
    }

    @Override
    protected CompletableFuture<Account> getReceiver(InternalTransferRequest transferRequest) {
        return accountService.getAccountByAccountNumber(transferRequest.getRecipientAccountNumber());
    }

    @Override
    protected void performTransfer(Account sender, Account receiver, InternalTransferRequest transferRequest) {
        String currencyCode = transferRequest.getCurrencyCode();
        BigDecimal transferAmount;

        Wallet senderWallet = getWallet(sender.getWallets(), currencyCode, transferRequest.getSourceAccountNumber(), true);
        Wallet receiverWallet = getWallet(receiver.getWallets(), currencyCode, transferRequest.getRecipientAccountNumber(), false);
        withdrawFromSender(senderWallet, transferRequest.getAmount(), currencyCode, transferRequest.getSourceAccountNumber());

        if (receiverWallet.getCurrency().getCurrencyCode().equals(currencyCode)) {
            transferAmount = receiverWallet.getBalance().add(transferRequest.getAmount());
        } else {
            transferAmount = currencyExchangeRateService.convert(transferRequest.getAmount(),
                    currencyCode,
                    receiverWallet.getCurrency().getCurrencyCode());
        }
        receiverWallet.setBalance(receiverWallet.getBalance().add(transferAmount));
        updateAccountsAndWallets(sender, receiver, senderWallet, receiverWallet);
    }

    @Override
    protected void createTransferTransaction(Account sender, Account receiver, InternalTransferRequest transferRequest) {
        Transaction transaction = buildTransaction(sender, transferRequest);
        createAndSaveMoneyTransfer(transaction, sender, receiver);
    }

    @Transactional
    private void createAndSaveMoneyTransfer(final Transaction transaction, final Account sender, final Account receiver) {
        transactionRepository.saveAndFlush(transaction);

        MoneyTransfer moneyTransfer = new MoneyTransfer();
        moneyTransfer.setTransactionId(transaction.getTransactionId());
        moneyTransfer.setTransaction(transaction);
        moneyTransfer.setReceiverAccount(sender);
        moneyTransfer.setSenderAccount(receiver);

        moneyTransferRepository.save(moneyTransfer);
    }

    private Transaction buildTransaction(Account sender, InternalTransferRequest transferRequest) {

        TransactionStatus status = transactionStatusRepository.findByStatusName("COMPLETED").orElseThrow(() -> new NotFoundException(String.format("TX Status not found: %s", "COMPLETED")));
        TransactionType type = transactionTypeRepository.findByTypeName("INTERNAL_TRANSFER").orElseThrow(() -> new NotFoundException(String.format("TX Type not found: %s", "INTERNAL_TRANSFER")));
        Currency currency = getCurrency(sender, transferRequest);
        Transaction transaction = new Transaction();
        transaction.setAmount(transferRequest.getAmount());
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setTransactionDate(OffsetDateTime.now());
        transaction.setDescription(transferRequest.getDescription());
        transaction.setFees(null);
        transaction.setCurrency(currency);

        return transaction;
    }

    @Override
    protected InternalTransferResponse buildTransferResponse(InternalTransferRequest transferRequest) {
        InternalTransferResponse response = new InternalTransferResponse();
        response.setTransferRequest(transferRequest);
        response.setStatus("success");
        return response;
    }

    @Transactional
    private void updateAccountsAndWallets(Account sender, Account receiver, Wallet senderWallet, Wallet receiverWallet) {
        walletService.updateBalance(senderWallet.getWalletId(), senderWallet.getBalance());
        walletService.updateBalance(receiverWallet.getWalletId(), receiverWallet.getBalance());

        // update total balance
        accountService.updateTotalBalance(sender);
        accountService.updateTotalBalance(receiver);
    }

    private Wallet getWallet(List<Wallet> wallets, String code, String an, boolean isSender) {
        Optional<Wallet> walletOptional = wallets.stream()
                .filter(wallet -> wallet.getCurrency().getCurrencyCode().equals(code))
                .findFirst();
        if (isSender) {
            return walletOptional.orElseThrow(() -> new WalletNotFoundForANException(code, an));
        }
        return walletOptional
                .orElse(wallets.stream()
                        .filter(Wallet::isMain)
                        .findFirst()
                        .orElseThrow(() -> new NoMainWalletException(code, an)));
    }

    private void withdrawFromSender(Wallet sender, BigDecimal amount, String currencyCode, String accountNumber) {
        if (amount.compareTo(sender.getBalance()) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Current balance: %s, transfer amount: %s, for wallet [%s, %s]",
                            sender.getBalance(),
                            amount,
                            currencyCode,
                            accountNumber));
        }
        sender.decreaseBalance(amount);
    }

    private Currency getCurrency(Account sender, InternalTransferRequest transferRequest) {
        return sender
                .getWallets()
                .stream()
                .map(Wallet::getCurrency)
                .filter(curr -> curr.getCurrencyCode().equals(transferRequest.getCurrencyCode()))
                .findFirst()
                .orElseThrow(() -> new CurrencyNotFoundException(
                        String.format("Currency %s not found in AN %s wallets",
                                transferRequest.getCurrencyCode(),
                                transferRequest.getRecipientAccountNumber())));
    }

}
