package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.model.account.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AccountService {

    CompletableFuture<Account> getAccount(final int accountId);
    CompletableFuture<Account> getAccountByAccountNumber(final String accountNumber);

    CompletableFuture<List<Account>> getAllAccounts();
    CompletableFuture<List<Account>> getAllAccountsWalletCount(final int walletCount);

    CompletableFuture<List<List<Account>>> getAllAccountsByWalletCountInRange(int origin, int bound);

    CompletableFuture<Account> addAccount(final Account account);

    String calculateTotalBalanceForAllAccounts();
    Account updateTotalBalance(final Account account);
    CompletableFuture<Account> calculateTotalBalanceForAccount(final Account account);

}
