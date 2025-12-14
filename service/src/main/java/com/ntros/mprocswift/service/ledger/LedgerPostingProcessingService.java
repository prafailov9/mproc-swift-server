package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerEntry;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.ledger.LedgerEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LedgerPostingProcessingService implements LedgerPostingService {

  private final LedgerEntryRepository ledgerEntryRepository;

  @Autowired
  public LedgerPostingProcessingService(LedgerEntryRepository ledgerEntryRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Override
  @Transactional
  public void postLedgerEntry(Transaction transaction, PostingEntry postingEntry) {
    LedgerEntryPair pair = createEntryPair(transaction, postingEntry);
    validateEntrySum(transaction, pair.debit, pair.credit);
    ledgerEntryRepository.saveAll(List.of(pair.debit, pair.credit));
  }

  @Override
  @Transactional
  public void postAll(Transaction transaction, List<PostingEntry> postings) {
    List<LedgerEntry> entries = new ArrayList<>();
    for (PostingEntry posting : postings) {
      LedgerEntryPair pair = createEntryPair(transaction, posting);
      entries.add(pair.debit);
      entries.add(pair.credit);
    }

    // 2) Validate double-entries (sum == 0 for each currency)
    Map<Integer, BigDecimal> totalsByCurrency = new HashMap<>();

    for (LedgerEntry entry : entries) {
      Integer currencyId = entry.getLedgerAccount().getCurrency().getCurrencyId();
      BigDecimal current = totalsByCurrency.getOrDefault(currencyId, BigDecimal.ZERO);
      totalsByCurrency.put(currencyId, current.add(entry.getAmount()));
    }

    for (Map.Entry<Integer, BigDecimal> e : totalsByCurrency.entrySet()) {
      BigDecimal sum = e.getValue();

      // Use compareTo(0) to ignore scale differences.
      if (sum.compareTo(BigDecimal.ZERO) != 0) {
        throw new IllegalStateException(
            "Unbalanced ledger postings for transaction "
                + transaction.getTransactionId()
                + " in currencyId="
                + e.getKey()
                + ". Total amount="
                + sum);
      }
    }

    ledgerEntryRepository.saveAll(entries);
  }

  private LedgerEntryPair createEntryPair(Transaction transaction, PostingEntry postingEntry) {
    LedgerAccount debitAccount = postingEntry.debitAccount();
    LedgerAccount creditAccount = postingEntry.creditAccount();

    // debit and credit should usually be same currency
    // TODO: add cross-currency support
    validateCurrencies(debitAccount, creditAccount);

    LedgerEntry debitEntry =
        buildLedgerEntry(transaction, postingEntry, true); // Debit: +amount, true
    LedgerEntry creditEntry =
        buildLedgerEntry(transaction, postingEntry, false); // Credit: -amount, false
    return new LedgerEntryPair(debitEntry, creditEntry);
  }

  private void validateCurrencies(LedgerAccount debitAccount, LedgerAccount creditAccount) {
    if (!debitAccount
        .getCurrency()
        .getCurrencyId()
        .equals(creditAccount.getCurrency().getCurrencyId())) {
      throw new IllegalStateException(
          "Debit and Credit accounts in a single PostingEntry must share the same currency. "
              + "Debit="
              + debitAccount.getCurrency().getCurrencyCode()
              + ", Credit="
              + creditAccount.getCurrency().getCurrencyCode());
    }
  }

  private void validateEntrySum(
      Transaction transaction, LedgerEntry debitEntry, LedgerEntry creditEntry) {
    BigDecimal sum = debitEntry.getAmount().add(creditEntry.getAmount());
    if (sum.compareTo(BigDecimal.ZERO) != 0) {
      throw new IllegalStateException(
          "Unbalanced ledger postings for transaction "
              + transaction.getTransactionId()
              + " in currencyId="
              + debitEntry.getLedgerAccount().getCurrency().getCurrencyCode()
              + ". Total amount="
              + sum);
    }
  }

  private LedgerEntry buildLedgerEntry(
      Transaction transaction, PostingEntry posting, boolean isDebit) {
    LedgerEntry entry = new LedgerEntry();
    String description = posting.description() != null ? posting.description() : "";

    entry.setTransaction(transaction);
    entry.setLedgerAccount(isDebit ? posting.debitAccount() : posting.creditAccount());
    entry.setAmount(isDebit ? posting.amount() : posting.amount().negate());
    entry.setDescription(description);
    entry.setEntryDate(OffsetDateTime.now());

    return entry;
  }

  private static final class LedgerEntryPair {
    LedgerEntry debit;
    LedgerEntry credit;

    LedgerEntryPair(LedgerEntry debit, LedgerEntry credit) {
      this.debit = debit;
      this.credit = credit;
    }
  }
}
