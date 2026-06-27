package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * service for managing data access for money transfer transactions
 */
public interface MoneyTransferService {

    List<MoneyTransfer> getAllTransfersForAccount(String accountNumber);

    List<MoneyTransfer> getAllWithdrawTransfersForAccount(String accountNumber);

    List<MoneyTransfer> getAllReceivedTransfersForAccount(String accountNumber);
}
