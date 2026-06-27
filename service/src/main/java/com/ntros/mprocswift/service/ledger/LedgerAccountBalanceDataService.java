package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import com.ntros.mprocswift.repository.ledger.LedgerAccountBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class LedgerAccountBalanceDataService implements LedgerAccountBalanceService {

  private final LedgerAccountBalanceRepository ledgerAccountBalanceRepository;

  @Autowired
  public LedgerAccountBalanceDataService(
      LedgerAccountBalanceRepository ledgerAccountBalanceRepository) {
    this.ledgerAccountBalanceRepository = ledgerAccountBalanceRepository;
  }

  @Override
  @Transactional
  public LedgerAccountBalance getLedgerAccountBalance(LedgerAccount ledgerAccount) {
    return getLedgerAccountBalance(ledgerAccount.getLedgerAccountId());
  }

  @Override
  public LedgerAccountBalance getLedgerAccountBalance(Integer ledgerAccountId) {
    return ledgerAccountBalanceRepository
        .findByIdForUpdate(ledgerAccountId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Balance not found for Ledger Account: %s", ledgerAccountId)));
  }

  @Override
  public int updateBalance(Integer ledgerAccountId, long delta) {
    return ledgerAccountBalanceRepository.updateBalance(ledgerAccountId, delta);
  }

  @Override
  public List<LedgerAccountBalance> getAllLedgerAccountBalances() {
    return ledgerAccountBalanceRepository.findAll();
  }

  @Override
  public List<LedgerAccountBalance> getAllHeldLedgerAccountBalances() {
    return ledgerAccountBalanceRepository.findAllHeld();
  }

  @Override
  public List<LedgerAccountBalance> getAllAvailableLedgerAccountBalances() {
    return ledgerAccountBalanceRepository.findAllAvailable();
  }

  @Override
  public boolean hasAvailableFunds(int walletId, long amount) {
    return ledgerAccountBalanceRepository.hasAvailableFunds(walletId, amount);
  }
}
