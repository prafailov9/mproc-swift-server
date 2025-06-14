package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.exceptions.AccountConstraintFailureException;
import com.ntros.mprocswift.exceptions.AccountNotFoundException;
import com.ntros.mprocswift.exceptions.NotFoundException;
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

import static java.lang.String.format;
import static java.math.RoundingMode.HALF_UP;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.springframework.util.CollectionUtils.isEmpty;


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
        return supplyAsync(() -> accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for id: " + accountId)));
    }

    @Override
    public CompletableFuture<Account> getAccountByAccountNumber(String accountNumber) {
        return supplyAsync(() -> accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for AN: " + accountNumber)));
    }

    @Override
    public CompletableFuture<List<Account>> getAllAccounts() {
        return supplyAsync(accountRepository::findAll);
    }

    @Override
    public CompletableFuture<List<Account>> getAllAccountsWalletCount(int walletCount) {
        return supplyAsync(() -> accountRepository.findAllByWalletCount(walletCount));

    }

    @Override
    public CompletableFuture<List<List<Account>>> getAllAccountsByWalletCountInRange(int origin, int bound) {
        return supplyAsync(() -> {
            int realBound = bound;
            List<Wallet> wallets = walletRepository.findAll();
            if (!isEmpty(wallets)) {
                realBound = wallets.size();
            }
            List<List<Account>> accountsByWalletCount = new ArrayList<>();
            int currentOrigin = origin;
            while (currentOrigin <= realBound) {
                List<Account> accounts = accountRepository.findAllByWalletCount(currentOrigin);
                if (!isEmpty(accounts)) {
                    accountsByWalletCount.add(accounts);
                }
                currentOrigin++;
            }
            return accountsByWalletCount;
        });
    }

    @Override
    public CompletableFuture<Account> createAccount(Account account) {
        return supplyAsync(() -> create(account));
    }

    @Override
    @Modifying
    @Transactional
    public String calculateTotalBalanceForAllAccounts() {
        List<Account> accounts = accountRepository.findAll()
                .stream()
                .filter(account -> !isEmpty(account.getWallets()))
                .toList();

        StringBuilder res = new StringBuilder();
        for (Account account : accounts) {
            List<Wallet> wallets = account.getWallets();
            if (wallets.size() == 1) {
                BigDecimal balance = wallets.get(0)
                        .getBalance()
                        .setScale(CurrencyUtils.getScale(wallets.get(0).getBalance()), HALF_UP);

                account.setTotalBalance(balance);
                res.append(format("Total balance for Account [ID: %s]=%s %s for 1 wallet\n",
                        account.getAccountId(), balance, wallets.get(0).getCurrency().getCurrencyCode()));
            } else {
                Wallet main = account.getMainWallet().orElseThrow(() -> new IllegalArgumentException(String.format("No wallet found for account: %s", account.getAccNumber())));
                main.setMain(true);
                wallets = wallets.stream().filter(wallet -> !wallet.isMain()).collect(Collectors.toList());
                BigDecimal amount = getTotal(wallets, account.getAccountId(), main);
                BigDecimal scaledAmount = amount.setScale(CurrencyUtils.getScale(amount), HALF_UP);
                account.setTotalBalance(scaledAmount);
                log.info("Saving total balance {} {}", account.getTotalBalance(), main.getCurrency().getCurrencyCode());
                // update balance
                accountRepository.saveAndFlush(account);
                res.append(format("Total balance for Account [ID: %s]=%s %s for %s wallets\n",
                        account.getAccountId(), account.getTotalBalance(), main.getCurrency().getCurrencyCode(), account.getWallets().size()));
            }
        }
        return res.toString();
    }

    @Override
    @Modifying
    @Transactional
    public Account updateTotalBalance(Account account) {
        List<Wallet> wallets = walletRepository.findAllByAccount(account.getAccountId());
        if (isEmpty(wallets)) {
            log.info("No wallets tied to account: {}", account.getAccountId());
            throw new NotFoundException(format(
                    "No wallets found for account: %s",
                    account.getAccountDetails().getAccountNumber()));
        }
        Wallet mainWallet = account.getMainWallet().orElseThrow(() -> new IllegalArgumentException(String.format("No wallet found for account: %s", account.getAccNumber())));
        BigDecimal totalAccountBalance = getTotal(wallets, account.getAccountId(), mainWallet);

        account.setTotalBalance(totalAccountBalance);
        log.info("Updated total balance for account: {}", account.getTotalBalance());
        accountRepository.save(account);
        return account;
    }

    @Override
    public CompletableFuture<Account> calculateTotalBalanceForAccount(final Account account) {
        return supplyAsync(() -> updateTotalBalance(account));
    }

    private Account create(Account account) {
        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException ex) {
            log.error("Could not save account {}. {}", account, ex.getMessage(), ex);
            throw new AccountConstraintFailureException(account);
        }
    }

    private BigDecimal getTotal(List<Wallet> wallets, int accountId, Wallet main) {
        if (isEmpty(wallets)) {
            log.info("No wallets for accountId: {}", accountId);
            throw new WalletNotFoundException(format("No wallets tied to accountId: %s", accountId));
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

}
