package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;

import java.util.List;

public interface LedgerAccountBalanceService {

  LedgerAccountBalance createLedgerAccountBalance(LedgerAccountBalance ledgerAccountBalance);

  LedgerAccountBalance getLedgerAccountBalance(LedgerAccount ledgerAccount);

  LedgerAccountBalance getLedgerAccountBalance(Integer ledgerAccountId);

  int updateBalance(Integer ledgerAccountId, long delta);

  List<LedgerAccountBalance> getAllLedgerAccountBalances();
  List<LedgerAccountBalance> getAllHeldLedgerAccountBalances();
  List<LedgerAccountBalance> getAllAvailableLedgerAccountBalances();
  List<LedgerAccountBalance> getAllSystemBalances();

  boolean hasAvailableFunds(int walletId, long minAllowedFunds);
}
