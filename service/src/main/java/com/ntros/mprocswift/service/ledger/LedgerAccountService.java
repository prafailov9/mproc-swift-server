package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.ledger.LedgerAccount;

import java.util.List;

public interface LedgerAccountService {

  LedgerAccount getAvailableForWallet(Wallet wallet);

  LedgerAccount getHeldForWallet(Wallet wallet);

  LedgerAccount getFxBridgeForCurrency(Currency currency);

  LedgerAccount getFeeIncomeForCurrency(Currency currency);

  List<LedgerAccount> getAllForWallet(Wallet wallet);

  List<LedgerAccount> getAllWalletLedgerAccounts();

  List<LedgerAccount> getAllMerchantLedgerAccounts();

  LedgerAccount getOrCreateWalletAccount(Wallet wallet, String typeCode);

  LedgerAccount getOrCreateMerchantSettlementAccount(Merchant merchant, Currency currency);

  LedgerAccount getOrCreateSystemAccount(String typeCode, Currency currency);
}
