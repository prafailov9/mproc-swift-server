package com.ntros.mprocswift.service.transfer;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.dto.transfer.W2WTransferResponse;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class W2WTransferService extends AbstractTransferService<W2WTransferRequest, W2WTransferResponse, Wallet> {

    @Override
    protected CompletableFuture<Wallet> getSender(W2WTransferRequest transferRequest) {
        return walletService.getWalletByCurrencyCodeAndAccountNumber(
                transferRequest.getCurrencyCode(),
                transferRequest.getSourceAccountNumber()
        );
    }

    @Override
    protected CompletableFuture<Wallet> getReceiver(W2WTransferRequest transferRequest) {
        return walletService.getWalletByCurrencyCodeAndAccountNumber(
                transferRequest.getToCurrencyCode(),
                transferRequest.getSourceAccountNumber()
        );
    }

    @Override
    protected void performTransfer(Wallet sender, Wallet receiver, W2WTransferRequest transferRequest) {
        sender.decreaseBalance(transferRequest.getAmount());

        BigDecimal convertedAmount = currencyExchangeRateService.convert(
                transferRequest.getAmount(),
                sender.getCurrency(),
                receiver.getCurrency()
        );

        receiver.increaseBalance(convertedAmount);
        updateWalletsAndAccounts(sender, receiver);
    }

    @Override
    protected void createTransferTransaction(Wallet sender, Wallet receiver, W2WTransferRequest transferRequest) {
        Transaction transaction = buildTransaction(sender, receiver, transferRequest);
        createAndSaveMoneyTransfer(transaction, sender, receiver);
    }

    private Transaction buildTransaction(Wallet sender, Wallet receiver, W2WTransferRequest transferRequest) {
        TransactionStatus status = transactionStatusRepository.findByStatusName("COMPLETED")
                .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));

        TransactionType type = transactionTypeRepository.findByTypeName("WALLET_TO_WALLET_TRANSFER")
                .orElseThrow(() -> new NotFoundException("TX Type not found: WALLET_TO_WALLET_TRANSFER"));

        Transaction tx = new Transaction();
        tx.setTransactionDate(OffsetDateTime.now());
        tx.setCurrency(sender.getCurrency());
        tx.setFees(null);
        tx.setAmount(transferRequest.getAmount());
        tx.setType(type);
        tx.setDescription(String.format(
                "Transferred %s from %s to %s.",
                transferRequest.getAmount(),
                sender.getCurrency().getCurrencyCode(),
                receiver.getCurrency().getCurrencyCode()
        ));
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

    private void updateWalletsAndAccounts(Wallet sender, Wallet receiver) {
        walletService.updateBalance(sender.getWalletId(), sender.getBalance());
        walletService.updateBalance(receiver.getWalletId(), receiver.getBalance());

        accountService.updateTotalBalance(sender.getAccount());
        accountService.updateTotalBalance(receiver.getAccount());
    }

    @Override
    protected W2WTransferResponse buildTransferResponse(W2WTransferRequest transferRequest) {
        W2WTransferResponse response = new W2WTransferResponse();
        response.setTransferRequest(transferRequest);
        response.setStatus("success");
        return response;
    }
}

