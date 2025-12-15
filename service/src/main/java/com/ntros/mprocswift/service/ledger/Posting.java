package com.ntros.mprocswift.service.ledger;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.ledger.LedgerAccount;

import java.math.BigDecimal;

public record Posting(
        LedgerAccount debitAccount,
        LedgerAccount creditAccount,
        BigDecimal amount,
        Currency currency,
        String description,
        String entryGroupKey
) {
}