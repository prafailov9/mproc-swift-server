package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.transactions.Transaction;

import java.util.List;

public interface LedgerPostingService {
    void postLedgerEntry(Transaction transaction, PostingEntry postingEntry);
    void postAll(Transaction transaction, List<PostingEntry> postings);

}
