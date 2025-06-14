package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;

import java.math.BigDecimal;

public record AuthPaymentContext(Card card,
                                 Merchant merchant,
                                 Wallet wallet,
                                 BigDecimal authorizedAmount,
                                 Currency requestedCurrency) {
}