package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.exceptions.AccountConstraintFailureException;
import com.ntros.mprocswift.exceptions.AccountNotFoundException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.exceptions.WalletNotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.currency.ConvertedAmount;
import com.ntros.mprocswift.repository.WalletRepository;
import com.ntros.mprocswift.repository.account.AccountRepository;
import com.ntros.mprocswift.service.currency.exchangerate.CurrencyExchangeRateDataService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.String.format;
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
  AccountDataService(
      final AccountRepository accountRepository,
      final WalletRepository walletRepository,
      final CurrencyExchangeRateDataService currencyExchangeRateDataService) {
    this.accountRepository = accountRepository;
    this.walletRepository = walletRepository;
    this.currencyExchangeRateDataService = currencyExchangeRateDataService;
  }

  @Override
  public List<Account> getAllAccountsByCurrencyCode(String currencyCode) {
    return accountRepository.findAllByWalletCurrencyCode(currencyCode);
  }

  @Override
  public CompletableFuture<Account> getAccount(int accountId) {
    return supplyAsync(
        () ->
            accountRepository
                .findById(accountId)
                .orElseThrow(
                    () -> new AccountNotFoundException("Account not found for id: " + accountId)));
  }

  @Override
  public CompletableFuture<Account> getAccountByAccountNumberAsync(String accountNumber) {
    return supplyAsync(
        () ->
            accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(
                    () ->
                        new AccountNotFoundException(
                            "Account not found for AN: " + accountNumber)));
  }

  @Override
  public Account getAccountByAccountNumber(String accountNumber) {
    return accountRepository
        .findByAccountNumber(accountNumber)
        .orElseThrow(
            () -> new AccountNotFoundException("Account not found for AN: " + accountNumber));
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
  public CompletableFuture<List<List<Account>>> getAllAccountsByWalletCountInRange(
      int origin, int bound) {
    return supplyAsync(
        () -> {
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
  @Transactional
  public Account updateTotalBalance(Account account) {
    throw new UnsupportedOperationException("Implement with ledger entries");
  }

  private Account create(Account account) {
    try {
      return accountRepository.save(account);
    } catch (DataIntegrityViolationException ex) {
      log.error("Could not save account {}. {}", account, ex.getMessage(), ex);
      throw new AccountConstraintFailureException(account);
    }
  }
}
