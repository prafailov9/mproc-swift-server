package com.ntros.mprocswift.service.ledger;

import static com.ntros.mprocswift.model.ledger.LedgerAccountTypeCode.*;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountType;
import com.ntros.mprocswift.repository.ledger.LedgerAccountRepository;
import com.ntros.mprocswift.repository.ledger.LedgerAccountTypeRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LedgerAccountDataService implements LedgerAccountService {

  private final LedgerAccountRepository ledgerAccountRepository;
  private final LedgerAccountTypeRepository ledgerAccountTypeRepository;

  @Autowired
  public LedgerAccountDataService(
      LedgerAccountRepository ledgerAccountRepository,
      LedgerAccountTypeRepository ledgerAccountTypeRepository) {
    this.ledgerAccountRepository = ledgerAccountRepository;
    this.ledgerAccountTypeRepository = ledgerAccountTypeRepository;
  }

  @Override
  public LedgerAccount getAvailableForWallet(Wallet wallet) {
    return getOrCreateWalletAccount(wallet, WALLET_AVAILABLE.name());
  }

  @Override
  public LedgerAccount getHeldForWallet(Wallet wallet) {
    return getOrCreateWalletAccount(wallet, WALLET_HELD.name());
  }

  @Override
  public LedgerAccount getFxBridgeForCurrency(Currency currency) {
    return getOrCreateSystemAccount(FX_BRIDGE.name(), currency);
  }

  @Override
  public LedgerAccount getFeeIncomeForCurrency(Currency currency) {
    return getOrCreateSystemAccount(FEE_INCOME.name(), currency);
  }

  @Override
  public List<LedgerAccount> getAllForWallet(Wallet wallet) {
    List<LedgerAccount> ledgerAccounts = ledgerAccountRepository.findAllByWallet(wallet);
    if (ledgerAccounts.isEmpty()) {
      throw new NotFoundException(
          String.format("Ledger Account not found for wallet [%s]", wallet));
    }
    return ledgerAccounts;
  }

  @Override
  public List<LedgerAccount> getAllWalletLedgerAccounts() {
    return ledgerAccountRepository.findAllWalletLedgerAccounts();
  }

  @Override
  public List<LedgerAccount> getAllMerchantLedgerAccounts() {
    return ledgerAccountRepository.findAllMerchantSettlementLedgerAccounts();
  }

  @Override
  @Transactional
  public LedgerAccount getOrCreateWalletAccount(Wallet wallet, String typeCode) {
    return ledgerAccountRepository
        .findOneByWalletLedgerTypeCurrencyCode(
            wallet, typeCode, wallet.getCurrency().getCurrencyCode())
        .orElseGet(
            () -> createWalletAccount(wallet, typeCode, wallet.getCurrency().getCurrencyCode()));
  }

  @Override
  public LedgerAccount getOrCreateMerchantSettlementAccount(Merchant merchant, Currency currency) {
    return ledgerAccountRepository
        .findOneByMerchantLedgerTypeCurrencyCode(
            merchant, MERCHANT_SETTLEMENT.name(), currency.getCurrencyCode())
        .orElseGet(() -> createMerchantAccount(merchant, MERCHANT_SETTLEMENT.name(), currency));
  }

  @Override
  public LedgerAccount getOrCreateSystemAccount(String typeCode, Currency currency) {
    return ledgerAccountRepository
        .findSystemAccount(typeCode, currency.getCurrencyCode())
        .orElseGet(() -> createSystemAccount(typeCode, currency));
  }

  private LedgerAccount createWalletAccount(Wallet wallet, String typeCode, String currencyCode) {
    LedgerAccountType ledgerAccountType =
        ledgerAccountTypeRepository
            .findByTypeCode(typeCode)
            .orElseThrow(
                () -> new IllegalStateException("Missing ledger_account_type: " + typeCode));

    LedgerAccount ledgerAccount = buildLedgerAccount(ledgerAccountType, wallet.getCurrency());
    ledgerAccount.setWallet(wallet);
    ledgerAccount.setLedgerAccountName(
        "Wallet " + wallet.getWalletId() + " " + typeCode + " (" + currencyCode + ")");

    try {
      return ledgerAccountRepository.saveAndFlush(ledgerAccount);
    } catch (DataIntegrityViolationException ex) {
      return ledgerAccountRepository
          .findOneByWalletLedgerTypeCurrencyCode(wallet, typeCode, currencyCode)
          .orElseThrow(() -> ex);
    }
  }

  private LedgerAccount createMerchantAccount(
      Merchant merchant, String typeCode, Currency currency) {
    LedgerAccountType ledgerAccountType =
        ledgerAccountTypeRepository
            .findByTypeCode(typeCode)
            .orElseThrow(
                () -> new IllegalStateException("Missing ledger_account_type: " + typeCode));

    LedgerAccount ledgerAccount = buildLedgerAccount(ledgerAccountType, currency);
    ledgerAccount.setMerchant(merchant);
    ledgerAccount.setLedgerAccountName(
        "Merchant "
            + merchant.getMerchantName()
            + " Settlement ("
            + currency.getCurrencyCode()
            + ")");

    try {
      return ledgerAccountRepository.saveAndFlush(ledgerAccount);
    } catch (DataIntegrityViolationException ex) {
      return ledgerAccountRepository
          .findOneByMerchantLedgerTypeCurrencyCode(merchant, typeCode, currency.getCurrencyCode())
          .orElseThrow(() -> ex);
    }
  }

  private LedgerAccount createSystemAccount(String typeCode, Currency currency) {
    LedgerAccountType type =
        ledgerAccountTypeRepository
            .findByTypeCode(typeCode)
            .orElseThrow(
                () -> new IllegalStateException("Missing ledger_account_type: " + typeCode));

    LedgerAccount acc = buildLedgerAccount(type, currency);
    acc.setLedgerAccountName("SYS " + typeCode + " (" + currency.getCurrencyCode() + ")");
    acc.setActive(true);

    try {
      return ledgerAccountRepository.saveAndFlush(acc);
    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
      return ledgerAccountRepository
          .findSystemAccount(typeCode, currency.getCurrencyCode())
          .orElseThrow(() -> ex);
    }
  }

  private LedgerAccount buildLedgerAccount(LedgerAccountType ledgerAccountType, Currency currency) {
    LedgerAccount ledgerAccount = new LedgerAccount();
    ledgerAccount.setLedgerAccountType(ledgerAccountType);
    ledgerAccount.setCurrency(currency);
    ledgerAccount.setActive(true);

    return ledgerAccount;
  }
}
