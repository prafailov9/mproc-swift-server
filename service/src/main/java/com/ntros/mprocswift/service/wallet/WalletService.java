package com.ntros.mprocswift.service.wallet;

import com.ntros.mprocswift.dto.UniqueWalletDTO;
import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.model.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WalletService {

    CompletableFuture<Wallet> getWallet(final int walletId);

    CompletableFuture<Wallet> getWalletByCurrencyCodeAndAccountNumber(final String currencyCode, final String accountNumber);

    CompletableFuture<Wallet> getWalletByCurrencyNameAndAccountId(final String currencyName, final int accountId);
    CompletableFuture<Wallet> getWalletByCurrencyCodeAndAccountId(final String currencyCode, final int accountId);

    /**
     * Blocking method, used in transfer service
     */
    // Wallet doGetWalletByCurrencyNameAndAccountId(final String currencyName, final int accountId);
    CompletableFuture<List<Wallet>> getAllWallets();
    CompletableFuture<List<Wallet>> getAllWalletsByAccount(final int accountId);
    CompletableFuture<Wallet> createWallet(final WalletDTO walletDTO);
    CompletableFuture<Wallet> createWallet(final Wallet wallet);

    CompletableFuture<Integer> deleteWallet(final UniqueWalletDTO uniqueWalletDTO);

    CompletableFuture<Void> updateBalance(final int walletId, final BigDecimal balance);

}
