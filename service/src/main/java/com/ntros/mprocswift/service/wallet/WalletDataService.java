package com.ntros.mprocswift.service.wallet;

import com.ntros.mprocswift.converter.WalletConverter;
import com.ntros.mprocswift.dto.UniqueWalletDTO;
import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.exceptions.*;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.repository.WalletRepository;
import com.ntros.mprocswift.repository.account.AccountRepository;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletDataService implements WalletService {

    protected static final Logger log = LoggerFactory.getLogger(WalletDataService.class);

    private final Executor executor;
    private final WalletRepository walletRepository;
    private final WalletConverter walletConverter;
    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;


    @Autowired
    public WalletDataService(@Qualifier("taskExecutor") Executor executor, final WalletRepository walletRepository,
                             final WalletConverter walletConverter,
                             final AccountRepository accountRepository,
                             final CurrencyRepository currencyRepository) {
        this.executor = executor;
        this.walletRepository = walletRepository;
        this.walletConverter = walletConverter;
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
    }

    @Override
    public Wallet getLockedWallet(int walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for id: " + walletId));
    }

    @Override
    public CompletableFuture<Wallet> getWallet(int walletId) {
        return CompletableFuture
                .supplyAsync(() -> walletRepository.findById(walletId)
                        .orElseThrow(() -> new WalletNotFoundException("Wallet not found for id: " + walletId)), executor);
    }

    @Override
    public CompletableFuture<Wallet> getWalletByCurrencyCodeAndAccountNumber(String currencyCode, String accountNumber) {
        return CompletableFuture
                .supplyAsync(() ->
                                walletRepository.findByCurrencyCodeAccountNumber(currencyCode, accountNumber)
                                        .orElseThrow(() ->
                                                new WalletNotFoundException(
                                                        String.format("Wallet not found for [%s, %s]", currencyCode, accountNumber)))
                        , executor);
    }

    @Override
    public CompletableFuture<Wallet> getWalletByCurrencyNameAndAccountId(String currencyName, int accountId) {
        return CompletableFuture
                .supplyAsync(() -> walletRepository.findByCurrencyNameAccountId(currencyName, accountId)
                        .orElseThrow(() -> new WalletNotFoundForCurrencyAndAccountException(currencyName, accountId)), executor);
    }

    @Override
    public CompletableFuture<Wallet> getWalletByCurrencyCodeAndAccountId(String currencyCode, int accountId) {
        return CompletableFuture
                .supplyAsync(() -> walletRepository.findByCurrencyCodeAccountId(currencyCode, accountId)
                        .orElseThrow(() -> new WalletNotFoundForCurrencyAndAccountException(currencyCode, accountId)), executor);
    }

    @Override
    public CompletableFuture<List<Wallet>> getAllWallets() {
        return CompletableFuture.supplyAsync(walletRepository::findAll, executor);
    }

    @Override
    public CompletableFuture<List<Wallet>> getAllWalletsByAccount(final int accountId) {
        return CompletableFuture
                .supplyAsync(() -> walletRepository.findAllByAccount(accountId), executor)
                .exceptionally(ex -> {
                    log.error(ex.getMessage(), ex.getCause());
                    throw new WalletNotFoundForAccountException(accountId, ex.getCause());
                });
    }

    @Override
    public CompletableFuture<Wallet> createWallet(WalletDTO walletDTO) {
        return CompletableFuture
                .supplyAsync(() -> walletConverter.toModel(walletDTO), executor)
                .thenApply(wallet ->
                        getAndSetCurrencyAccount(walletDTO.getCurrencyCode(), walletDTO.getAccountNumber(), wallet))
                .thenApply(this::updateActiveWallet)
                .thenApply(this::create);
    }

    @Override
    public CompletableFuture<Wallet> createWallet(Wallet wallet) {
        return CompletableFuture.supplyAsync(() -> create(wallet));
    }

    @Override
    public CompletableFuture<Integer> deleteWallet(UniqueWalletDTO uniqueWalletDTO) {
        return CompletableFuture
                .supplyAsync(() -> delete(uniqueWalletDTO.getCurrencyCode(), uniqueWalletDTO.getAccountNumber()), executor);
    }

    @Override
    public CompletableFuture<Void> updateBalance(int walletId, BigDecimal balance) {
        return CompletableFuture.runAsync(() -> {
            try {
                walletRepository.updateBalance(walletId, balance);
            } catch (DataAccessException ex) {
                String error = String.format("Could not update balance for wallet: %s", walletId);
                log.error(error, ex);
                // rethrowing as a completion exception so it is caught by exceptionally block in thenCombine
                throw new CompletionException(error, new WalletUpdateFailedException(error));
            }
        }, executor);
    }

    private int delete(String code, String an) {
        int affectedRows;
        try {
            affectedRows = walletRepository.deleteByCurrencyCodeAccountNumber(code, an);
            if (affectedRows > 1) {
                log.error("Modified {} rows after delete.", affectedRows);
                throw new DataConstraintViolationException(String.format("Deleted multiple wallets for [%s, %s]", code, an));
            }
            log.error("Successfully deleted {} wallet for account: {}", code, an);
        } catch (DataAccessException ex) {
            log.error("Could not delete wallet for {}, {}", code, an, ex);
            throw new WalletDeleteFailedException(code, an, ex);
        }
        return affectedRows;
    }

    private Wallet getAndSetCurrencyAccount(String currencyCode, String accountNumber, Wallet wallet) {
        Currency currency = currencyRepository.findByCurrencyCode(currencyCode).orElseThrow(() ->
                new CurrencyNotFoundException(String.format("Currency [%s] doesnt exist.", currencyCode)));
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() ->
                new AccountNotFoundException(String.format("Account with AN:[%s] doesnt exist.", accountNumber)));

        wallet.setCurrency(currency);
        wallet.setAccount(account);
        return wallet;
    }

    private Wallet updateActiveWallet(Wallet wallet) {
        if (wallet.isMain()) {
            wallet.getAccount().setWallets(wallet.getAccount().getWallets()
                    .stream()
                    .peek(w -> w.setMain(Boolean.FALSE))
                    .collect(Collectors.toList()));
            accountRepository.saveAndFlush(wallet.getAccount());
        }
        return wallet;
    }

    private Wallet create(Wallet wallet) {
        try {
            return walletRepository.save(wallet);
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to save wallet: {}", wallet);
            throw new WalletCreateFailedException(String.format("Could not create wallet for currency and account [%s, %s]",
                    wallet.getCurrency().getCurrencyCode(),
                    wallet.getAccount().getAccountDetails().getAccountNumber()),
                    ex.getCause());
        }
    }
}
