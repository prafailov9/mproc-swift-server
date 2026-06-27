package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionQuote;

public record AuthPaymentContext(
    Card card,
    Merchant merchant,
    Wallet wallet,
    long authorizedAmount,
    Currency requestedCurrency,
    ConversionQuote conversionQuote) {}
