package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;

public interface LedgerAccountBalanceService {
  LedgerAccountBalance getLedgerAccountBalance(LedgerAccount ledgerAccount);
  LedgerAccountBalance getLedgerAccountBalance(Integer ledgerAccountId);
  int updateBalance(Integer ledgerAccountId, long delta);
}
