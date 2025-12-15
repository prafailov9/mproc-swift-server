package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.Transaction;

import java.util.List;

public interface LedgerEntryService {

  void createLedgerEntries(Transaction transaction, List<Posting> postings);
}
