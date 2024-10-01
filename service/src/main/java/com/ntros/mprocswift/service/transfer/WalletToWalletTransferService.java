package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.WalletToWalletTransferRequest;
import com.ntros.mprocswift.dto.transfer.WalletToWalletTransferResponse;
import com.ntros.mprocswift.model.Wallet;
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
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class WalletToWalletTransferService extends AbstractTransferService<WalletToWalletTransferRequest, WalletToWalletTransferResponse, Wallet> {

    @Override
    protected CompletableFuture<Wallet> getSender(WalletToWalletTransferRequest transferRequest) {
        return walletService
                .getWalletByCurrencyCodeAndAccountNumber(
                        transferRequest.getCurrencyCode(),
                        transferRequest.getSourceAccountNumber());
    }

    @Override
    protected CompletableFuture<Wallet> getReceiver(WalletToWalletTransferRequest transferRequest) {
        return walletService
                .getWalletByCurrencyCodeAndAccountNumber(
                        transferRequest.getToCurrencyCode(),
                        transferRequest.getSourceAccountNumber());
    }

    @Override
    protected CompletableFuture<Void> performTransfer(Wallet sender, Wallet receiver, WalletToWalletTransferRequest transferRequest) {
        return CompletableFuture.runAsync(() -> {
            sender.setBalance(sender.getBalance().subtract(transferRequest.getAmount()));

            BigDecimal convertedAmount = currencyExchangeRateService.convert(
                    transferRequest.getAmount(),
                    sender.getCurrency(),
                    receiver.getCurrency());

            receiver.setBalance(receiver.getBalance().add(convertedAmount));

            updateWalletsAndAccounts(sender, receiver);
        }, executor);
    }

    @Override
    @Modifying
    @Transactional
    protected CompletableFuture<Void> createTransferTransaction(Wallet sender, Wallet receiver, WalletToWalletTransferRequest transferRequest) {
        return CompletableFuture.runAsync(() -> {
            Transaction transaction = new Transaction();
            transaction.setTransactionDate(OffsetDateTime.now());
            transaction.setCurrency(sender.getCurrency());
            transaction.setFees(null);
            transaction.setAmount(transferRequest.getAmount());
            transaction.setType(TransactionType.WALLET_TO_WALLET_TRANSFER);
            transaction.setDescription(String.format("Transferred %s from %s to %s.",
                    transferRequest.getAmount(),
                    sender.getCurrency().getCurrencyCode(),
                    receiver.getCurrency().getCurrencyCode()));
            transaction.setStatus(TransactionStatus.COMPLETED);

            // a base transaction must exist before saving a money transfer
            transaction = transactionRepository.saveAndFlush(transaction);

            MoneyTransfer moneyTransfer = new MoneyTransfer();
            moneyTransfer.setTransactionId(transaction.getTransactionId());
            moneyTransfer.setTransaction(transaction);
            // sender and receiver wallets should be of the same account
            moneyTransfer.setSenderAccount(sender.getAccount());
            moneyTransfer.setReceiverAccount(receiver.getAccount());
            moneyTransfer.setTargetCurrencyCode(receiver.getCurrency().getCurrencyCode());
            moneyTransferRepository.save(moneyTransfer);
        }, executor);
    }


    @Modifying
    @Transactional
    private void updateWalletsAndAccounts(final Wallet sender, final Wallet receiver) {
        walletService.updateBalance(sender.getWalletId(), sender.getBalance());
        walletService.updateBalance(receiver.getWalletId(), receiver.getBalance());

        accountService.updateTotalBalance(sender.getAccount());
        accountService.updateTotalBalance(receiver.getAccount());
    }

    @Override
    protected CompletableFuture<WalletToWalletTransferResponse> buildTransferResponse(WalletToWalletTransferRequest transferRequest) {
        return CompletableFuture.supplyAsync(() -> {
            WalletToWalletTransferResponse response = new WalletToWalletTransferResponse();
            response.setTransferRequest(transferRequest);
            response.setStatus("success");
            return response;
        }, executor);
    }

}
