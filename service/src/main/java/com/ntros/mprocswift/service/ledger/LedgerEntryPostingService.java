package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import com.ntros.mprocswift.model.ledger.LedgerEntry;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.ledger.LedgerEntryRepository;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LedgerEntryPostingService implements LedgerEntryService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final LedgerAccountBalanceService ledgerAccountBalanceService;

  @Autowired
  public LedgerEntryPostingService(
      LedgerEntryRepository ledgerEntryRepository,
      LedgerAccountBalanceService ledgerAccountBalanceService) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.ledgerAccountBalanceService = ledgerAccountBalanceService;
  }

  @Override
  @Transactional
  public void createLedgerEntries(Transaction transaction, List<Posting> postings) {
    if (transaction.getTransactionId() == null) {
      throw new IllegalArgumentException(
          "Transaction must be persisted before posting ledger entries.");
    }

    List<LedgerEntry> entries = populateEntries(transaction, postings);

    // entries are grouped by currency for the entire posting for each currency, group all of its
    // entries and sum the amount
    Map<Integer, Long> totalsByCurrency = getTotalsByCurrency(entries);
    // validate double-entries (sum == 0 for each currency)
    validateBalancedLedgersByCurrency(transaction, totalsByCurrency);

    ledgerEntryRepository.saveAll(entries);

    // apply to balances (group by ledger_account_id)
    Map<Integer, Long> deltaByAccountId = new HashMap<>();
    for (LedgerEntry e : entries) {
      int id = e.getLedgerAccount().getLedgerAccountId();
      deltaByAccountId.merge(id, e.getAmount(), Long::sum);
    }

    // lock/apply in deterministic order to avoid deadlocks
    List<Integer> ids = new ArrayList<>(deltaByAccountId.keySet());
    Collections.sort(ids);

    for (Integer ledgerAccountId : ids) {
      LedgerAccountBalance lockedBalance =
          ledgerAccountBalanceService.getLedgerAccountBalance(ledgerAccountId);
      if (lockedBalance == null) {
        throw new NotFoundException("Missing balance row for ledgerAccountId=" + ledgerAccountId);
      }
      log.info("locking balance: {}", lockedBalance);

      long delta = deltaByAccountId.get(ledgerAccountId);
      ledgerAccountBalanceService.updateBalance(ledgerAccountId, delta);
    }
  }

  private void validateBalancedLedgersByCurrency(
      Transaction txn, Map<Integer, Long> totalsByCurrency) {
    for (Map.Entry<Integer, Long> e : totalsByCurrency.entrySet()) {
      long sum = e.getValue();

      // Use compareTo(0) to ignore scale differences.
      if (sum != 0) {
        throw new IllegalStateException(
            "Unbalanced ledger postings for transaction "
                + txn.getTransactionId()
                + " in currencyId="
                + e.getKey()
                + ". Total amount="
                + sum);
      }
    }
  }

  private Map<Integer, Long> getTotalsByCurrency(List<LedgerEntry> entries) {
    Map<Integer, Long> totalsByCurrency = new HashMap<>();
    for (LedgerEntry entry : entries) {
      Integer currencyId = entry.getLedgerAccount().getCurrency().getCurrencyId();

      long currentSum = 0;
      if (totalsByCurrency.containsKey(currencyId)) {
        currentSum = totalsByCurrency.get(currencyId);
      }

      totalsByCurrency.put(currencyId, currentSum + entry.getAmount());
    }
    return totalsByCurrency;
  }

  private List<LedgerEntry> populateEntries(Transaction transaction, List<Posting> postings) {
    List<LedgerEntry> entries = new ArrayList<>();

    for (Posting posting : postings) {
      validatePosting(posting);
      // Debit: +amount, true
      long debit = posting.amount();
      // Credit: -amount, false
      long credit = posting.amount() * (-1);

      LedgerEntry debitEntry =
          buildLedgerEntry(
              transaction, posting, posting.debitAccount(), debit, posting.entryGroupKey());

      LedgerEntry creditEntry =
          buildLedgerEntry(
              transaction, posting, posting.creditAccount(), credit, posting.entryGroupKey());
      entries.add(debitEntry);
      entries.add(creditEntry);
    }

    return entries;
  }

  private LedgerEntry buildLedgerEntry(
      Transaction transaction,
      Posting posting,
      LedgerAccount ledgerAccount,
      long amountMinorUnits,
      String entryGroupKey) {

    //    long normalized = Money.toMinor(amount, ledgerAccount.getCurrency().getMinorUnits());
    LedgerEntry entry = new LedgerEntry();
    entry.setEntryGroupKey(entryGroupKey);
    entry.setTransaction(transaction);
    entry.setLedgerAccount(ledgerAccount);
    entry.setAmount(amountMinorUnits);
    entry.setDescription(
        posting.description() != null ? posting.description() : "[Template_Description]");
    entry.setEntryDate(OffsetDateTime.now());

    return entry;
  }

  private void validatePosting(Posting posting) {
    var d = posting.debitAccount();
    var c = posting.creditAccount();

    if (!d.getCurrency().getCurrencyId().equals(c.getCurrency().getCurrencyId())) {
      throw new IllegalStateException("Debit/Credit accounts must be same currency per Posting.");
    }

    if (posting.amount() <= 0) {
      throw new IllegalArgumentException("Posting amount must be > 0.");
    }
  }
}
