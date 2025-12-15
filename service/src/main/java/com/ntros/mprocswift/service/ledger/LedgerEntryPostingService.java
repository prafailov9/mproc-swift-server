package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerEntry;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.ledger.LedgerEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  @Autowired
  public LedgerEntryPostingService(LedgerEntryRepository ledgerEntryRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Override
  @Transactional
  public void createLedgerEntries(Transaction transaction, List<Posting> postings) {
    if (transaction.getTransactionId() == null) {
      throw new IllegalArgumentException(
          "Transaction must be persisted before posting ledger entries.");
    }

    List<LedgerEntry> entries = populateEntries(transaction, postings);

    // entries are grouped by currency for the entire posting
    // for each currency, group all of its entries and sum the amount
    Map<Integer, BigDecimal> totalsByCurrency = getTotalsByCurrency(entries);

    // validate double-entries (sum == 0 for each currency)
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

  private Map<Integer, BigDecimal> getTotalsByCurrency(List<LedgerEntry> entries) {
    Map<Integer, BigDecimal> totalsByCurrency = new HashMap<>();
    for (LedgerEntry entry : entries) {
      Integer currencyId = entry.getLedgerAccount().getCurrency().getCurrencyId();

      BigDecimal currentSum = BigDecimal.ZERO;
      if (totalsByCurrency.containsKey(currencyId)) {
        currentSum = totalsByCurrency.get(currencyId);
      }

      totalsByCurrency.put(currencyId, currentSum.add(entry.getAmount()));
    }
    return totalsByCurrency;
  }

  private List<LedgerEntry> populateEntries(Transaction transaction, List<Posting> postings) {
    List<LedgerEntry> entries = new ArrayList<>();

    for (Posting posting : postings) {
      validatePosting(posting);
      // Debit: +amount, true
      BigDecimal debit = posting.amount();
      // Credit: -amount, false
      BigDecimal credit = posting.amount().negate();

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
      BigDecimal amount,
      String entryGroupKey) {

    BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
    LedgerEntry entry = new LedgerEntry();
    entry.setEntryGroupKey(entryGroupKey);
    entry.setTransaction(transaction);
    entry.setLedgerAccount(ledgerAccount);
    entry.setAmount(normalized);
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

    if (posting.amount() == null || posting.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Posting amount must be > 0.");
    }
  }
}
