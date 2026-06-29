package com.ntros.mprocswift.service.account;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.repository.WalletRepository;
import com.ntros.mprocswift.repository.account.AccountRepository;
import com.ntros.mprocswift.repository.ledger.LedgerAccountBalanceRepository;
import com.ntros.mprocswift.repository.ledger.LedgerEntryRepository;
import com.ntros.mprocswift.service.currency.exchangerate.FxConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountBalanceUpdaterService implements AccountUpdaterService {
  private final AccountRepository accountRepository;
  private final WalletRepository walletRepository;
  private final LedgerAccountBalanceRepository balanceRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final FxConversionService fxConversionService;

  @Autowired
  public AccountBalanceUpdaterService(
      AccountRepository accountRepository,
      WalletRepository walletRepository,
      LedgerAccountBalanceRepository balanceRepository,
      LedgerEntryRepository ledgerEntryRepository,
      FxConversionService fxConversionService) {
    this.accountRepository = accountRepository;
    this.walletRepository = walletRepository;
    this.balanceRepository = balanceRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.fxConversionService = fxConversionService;
  }

  /**
   * Updates the account's available balance across all levels, from entries or balance cache table
   */
  @Transactional
  @Override
  public Account updateAndFetchWalletAccount(String accountNumber) {
    var account =
        accountRepository
            .findByAccountNumber(accountNumber)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Account for accountNumber:%s Not Found", accountNumber)));

    // get ledger accounts for selected account
    var balances = balanceRepository.findAllForAccountNumber(accountNumber);
    // update balances on ledger/wallet level
    for (var b : balances) {
      var ledgerAccount = b.getLedgerAccount();
      var type = ledgerAccount.getLedgerAccountType();
      int walletId = ledgerAccount.getWallet().getWalletId();
      // reconstruct entries for account
      var entries = ledgerEntryRepository.findAllByLedgerAccount(ledgerAccount);
      if (entries.isEmpty()) {
        // reconstruct only from balance row
        updateWalletBalance(account, walletId, b.getBalanceMinor());
      } else if (ledgerAccount.getWallet() != null && type.getTypeCode().contains("AVAILABLE")) {
        long availableSum = 0L;
        for (var e : entries) {
          availableSum += e.getAmount();
        }
        b.setBalanceMinor(availableSum);
        balanceRepository.updateBalance(ledgerAccount.getLedgerAccountId(), availableSum);
        ledgerAccount.setBalance(b);
        updateWalletBalance(account, walletId, availableSum);
      }
    }
    var mainWallet = account.getMainWallet().orElseThrow();
    var conversionWallets = account.getWallets().stream().filter(Wallet::isMain).toList();

    // compute total balance across currencies
    long runningTotal = mainWallet.getBalance();
    for (var w : conversionWallets) {
      var balance = w.getBalance();
      var quote = fxConversionService.convert(balance, w.getCurrency(), mainWallet.getCurrency());
      runningTotal += quote.targetMoney().minorAmount();
    }

    account.setTotalBalance(runningTotal);
    return account;
  }

  private void updateWalletBalance(Account account, int walletId, long availableSum) {
    var wallet = account.getWallet(walletId).orElseThrow();
    walletRepository.updateBalance(walletId, availableSum);
    wallet.setBalance(availableSum);
  }
}
