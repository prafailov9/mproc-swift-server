package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;

import java.util.List;

public interface LedgerAccountBalanceService {
  LedgerAccountBalance getLedgerAccountBalance(LedgerAccount ledgerAccount);

  LedgerAccountBalance getLedgerAccountBalance(Integer ledgerAccountId);

  int updateBalance(Integer ledgerAccountId, long delta);

  List<LedgerAccountBalance> getAllLedgerAccountBalances();
  List<LedgerAccountBalance> getAllHeldLedgerAccountBalances();
  List<LedgerAccountBalance> getAllAvailableLedgerAccountBalances();
  boolean hasAvailableFunds(int walletId, long minAllowedFunds);
}
