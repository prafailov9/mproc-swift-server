package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.model.currency.Currency;

import java.util.concurrent.CompletableFuture;

public interface CurrencyService {


    CompletableFuture<Currency> getCurrencyByCodeAsync(String code);

    Currency getCurrencyByCode(String code);

    CompletableFuture<Void> activateAll();

    CompletableFuture<Void> deleteCurrency(final int currencyId);

}
