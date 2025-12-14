package com.ntros.mprocswift.repository.ledger;

import com.ntros.mprocswift.model.ledger.LedgerEntry;
import com.ntros.mprocswift.model.transactions.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Integer> {

    List<LedgerEntry> findAllByTransaction(Transaction transaction);

}
