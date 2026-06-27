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
      throw new IllegalArgumentException("Transaction must exist before posting ledger entries.");
    }
    List<LedgerEntry> entries = populateEntries(transaction, postings);
    log.info("Assembled ledger entries for processing: {}", entries);
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
    List<Integer> accountIds = new ArrayList<>(deltaByAccountId.keySet());
    Collections.sort(accountIds);

    for (Integer ledgerAccountId : accountIds) {
      LedgerAccountBalance lockedBalance = getLockedBalance(ledgerAccountId, postings);
      log.info("locking balance: {}", lockedBalance);

      long delta = deltaByAccountId.get(ledgerAccountId);
      ledgerAccountBalanceService.updateBalance(ledgerAccountId, delta);
    }
  }

  @Override
  public List<LedgerEntry> getAllEntries() {
    return ledgerEntryRepository.findAll();
  }

  @Override
  public List<LedgerEntry> getAllForTransaction(Transaction transaction) {
    return ledgerEntryRepository.findAllByTransaction(transaction);
  }

  private LedgerAccountBalance getLockedBalance(int ledgerAccountId, List<Posting> postings) {
    // attempt locked balance read
    try {
      return ledgerAccountBalanceService.getLedgerAccountBalance(ledgerAccountId);
    } catch (NotFoundException ex) {
      log.error(
          "Balance for ledgerAccountId:{} not found. Creating Balance row...", ledgerAccountId);
    }

    // thus far, we know the account exists but its balance row is missing, most probably for a
    // FX_BRIDGE system account for a non-base currency(USD, EUR).
    var balance = createBalanceForLedgerAccount(ledgerAccountId, postings);
    // create the balance
    ledgerAccountBalanceService.createLedgerAccountBalance(balance);
    log.info("Balance row created. Attempting read again");
    return ledgerAccountBalanceService.getLedgerAccountBalance(ledgerAccountId);
  }

  private LedgerAccountBalance createBalanceForLedgerAccount(
      int ledgerAccountId, List<Posting> postings) {
    var acc = new LedgerAccount();
    // find the account from the postings
    for (var posting : postings) {
      if (posting.creditAccount().getLedgerAccountId() == ledgerAccountId) {
        acc = posting.creditAccount();
        break;
      }
      if (posting.debitAccount().getLedgerAccountId() == ledgerAccountId) {
        acc = posting.debitAccount();
        break;
      }
    }
    var balance = new LedgerAccountBalance();
    balance.setLedgerAccount(acc);
    balance.setBalanceMinor(0);
    return balance;
  }

  private void validateBalancedLedgersByCurrency(
      Transaction txn, Map<Integer, Long> totalsByCurrency) {
    for (Map.Entry<Integer, Long> e : totalsByCurrency.entrySet()) {
      Long sum = e.getValue();

      if (sum.compareTo(0L) != 0) {
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

    // entry sequence for the entire posting list
    int seq = 0;
    for (Posting posting : postings) {
      validatePosting(posting);
      // Debit: +amount, true
      long debit = posting.amount();
      // Credit: -amount, false
      long credit = posting.amount() * (-1);

      LedgerEntry debitEntry =
          buildLedgerEntry(
              transaction, posting, posting.debitAccount(), debit, posting.entryGroupKey(), ++seq);

      LedgerEntry creditEntry =
          buildLedgerEntry(
              transaction,
              posting,
              posting.creditAccount(),
              credit,
              posting.entryGroupKey(),
              ++seq);
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
      String entryGroupKey,
      int entrySeq) {

    //    long normalized = Money.toMinor(amount, ledgerAccount.getCurrency().getMinorUnits());
    LedgerEntry entry = new LedgerEntry();
    entry.setEntryGroupKey(entryGroupKey);
    entry.setEntrySequence(entrySeq);
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
