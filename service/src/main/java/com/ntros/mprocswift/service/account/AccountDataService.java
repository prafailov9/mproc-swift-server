package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.exceptions.AccountConstraintFailureException;
import com.ntros.mprocswift.exceptions.AccountNotFoundException;
import com.ntros.mprocswift.exceptions.WalletNotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.repository.WalletRepository;
import com.ntros.mprocswift.repository.account.AccountRepository;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateDataService;
import com.ntros.mprocswift.service.currency.CurrencyUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountDataService implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountDataService.class);

    private final AccountRepository accountRepository;

    private final WalletRepository walletRepository;

    private final CurrencyExchangeRateDataService currencyExchangeRateDataService;

    @Autowired
    AccountDataService(final AccountRepository accountRepository,
                       final WalletRepository walletRepository,
                       final CurrencyExchangeRateDataService currencyExchangeRateDataService) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.currencyExchangeRateDataService = currencyExchangeRateDataService;
    }

    @Override
    public CompletableFuture<Account> getAccount(int accountId) {
        return CompletableFuture
                .supplyAsync(() -> accountRepository.findById(accountId)
                        .orElseThrow(() -> new AccountNotFoundException("Account not found for id: " + accountId)));
    }

    @Override
    public CompletableFuture<Account> getAccountByAccountNumber(String accountNumber) {
        return CompletableFuture
                .supplyAsync(() -> accountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new AccountNotFoundException("Account not found for AN: " + accountNumber)));
    }

    @Override
    public CompletableFuture<List<Account>> getAllAccounts() {
        return CompletableFuture.supplyAsync(accountRepository::findAll);
    }

    @Override
    public CompletableFuture<List<Account>> getAllAccountsWalletCount(int walletCount) {
        return CompletableFuture.supplyAsync(() -> accountRepository.findAllByWalletCount(walletCount));

    }

    @Override
    public CompletableFuture<List<List<Account>>> getAllAccountsByWalletCountInRange(int origin, int bound) {
        return CompletableFuture
                .supplyAsync(() -> {
                    int realBound = bound;
                    List<Wallet> wallets = walletRepository.findAll();
                    if (!CollectionUtils.isEmpty(wallets)) {
                        realBound = wallets.size();
                    }
                    List<List<Account>> accountsByWalletCount = new ArrayList<>();
                    int currentOrigin = origin;
                    while (currentOrigin <= realBound) {
                        List<Account> accounts = accountRepository.findAllByWalletCount(currentOrigin);
                        if (!CollectionUtils.isEmpty(accounts)) {
                            accountsByWalletCount.add(accounts);
                        }
                        currentOrigin++;
                    }
                    return accountsByWalletCount;
                });
    }

    @Override
    public CompletableFuture<Account> createAccount(Account account) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return accountRepository.save(account);
                    } catch (DataIntegrityViolationException ex) {
                        log.error("Could not save account {}. {}", account, ex.getMessage(), ex);
                        throw new AccountConstraintFailureException(account);
                    }
                });
    }

    @Override
    @Modifying
    @Transactional
    public String calculateTotalBalanceForAllAccounts() {
        List<Account> accounts = accountRepository.findAll()
                .stream()
                .filter(account -> !CollectionUtils.isEmpty(account.getWallets()))
                .toList();
        StringBuilder res = new StringBuilder();
        for (Account account : accounts) {
            List<Wallet> wallets = account.getWallets();
            if (wallets.size() == 1) {
                BigDecimal balance = wallets.get(0)
                        .getBalance()
                        .setScale(CurrencyUtils.getScale(wallets.get(0).getBalance()), RoundingMode.HALF_UP);

                account.setTotalBalance(balance);
                res.append(String.format("Total balance for Account [ID: %s]=%s %s for 1 wallet\n",
                        account.getAccountId(), balance, wallets.get(0).getCurrency().getCurrencyCode()));
            } else {
                // get main currency wallet or set 1st to main
                Wallet main = getOrSetMainWallet(wallets);
                main.setMain(true);
                wallets = wallets.stream().filter(wallet -> !wallet.isMain()).collect(Collectors.toList());
                BigDecimal amount = getTotal(wallets, account.getAccountId(), main);
                BigDecimal scaledAmount = amount.setScale(CurrencyUtils.getScale(amount), RoundingMode.HALF_UP);
                account.setTotalBalance(scaledAmount);
                log.info("Saving total balance {} {}", account.getTotalBalance(), main.getCurrency().getCurrencyCode());
                // update balance
                accountRepository.saveAndFlush(account);
                res.append(String.format("Total balance for Account [ID: %s]=%s %s for %s wallets\n",
                        account.getAccountId(), account.getTotalBalance(), main.getCurrency().getCurrencyCode(), account.getWallets().size()));
            }
        }
        return res.toString();
    }

    @Override
    public Account updateTotalBalance(Account account) {
        List<Wallet> wallets = walletRepository.findAllByAccount(account.getAccountId());
        if (CollectionUtils.isEmpty(wallets)) {
            log.info("No wallets tied to account: {}", account.getAccountId());
            return null;
        }
        Wallet mainWallet = getOrSetMainWallet(wallets);
        BigDecimal totalAccountBalance = getTotal(wallets, account.getAccountId(), mainWallet);

        account.setTotalBalance(totalAccountBalance);
        log.info("Updated total balance for account: {}", account.getTotalBalance());
        accountRepository.saveAndFlush(account);
        return account;
    }

    @Override
    @Modifying
    @Transactional
    public CompletableFuture<Account> calculateTotalBalanceForAccount(final Account account) {
        return CompletableFuture
                .supplyAsync(() -> updateTotalBalance(account))
                .thenComposeAsync(this::createAccount);
    }

    private BigDecimal getTotal(List<Wallet> wallets, int accountId, Wallet main) {
        if (CollectionUtils.isEmpty(wallets)) {
            log.info("No wallets for accountId: {}", accountId);
            throw new WalletNotFoundException(String.format("No wallets tied to accountId: %s", accountId));
        }
        // convert each to main currency and add
        return wallets
                .stream()
                .map(wallet -> currencyExchangeRateDataService.convert(
                        wallet.getBalance(),
                        wallet.getCurrency(),
                        main.getCurrency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Wallet getOrSetMainWallet(List<Wallet> wallets) {
        Wallet main = null;
        for (Wallet wallet: wallets) {
            if (wallet.isMain()) {
                if (main == null) {
                    main = wallet;
                } else {
                    wallet.setMain(false);
                }
            }
        }
        if (main == null) {
            main = wallets.get(0);
            main.setMain(true);
        }
        return main;
    }

}
