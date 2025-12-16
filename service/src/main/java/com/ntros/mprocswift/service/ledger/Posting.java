package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccount;

public record Posting(
    LedgerAccount debitAccount,
    LedgerAccount creditAccount,
    long amount,
    String description,
    String entryGroupKey) {}
