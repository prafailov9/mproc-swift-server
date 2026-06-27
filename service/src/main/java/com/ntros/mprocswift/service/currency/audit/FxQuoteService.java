package com.ntros.mprocswift.service.currency.audit;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;

import java.util.List;

public interface FxQuoteService {

  FxQuote getQuoteByTransaction(Transaction transaction);

  FxQuote createFxQuote(FxQuote fxQuote);

  List<FxQuote> getAllQuotesForCurrencies(Currency source, Currency target);

  List<FxQuote> getAllQuotesForCurrencyCodes(String sourceCode, String targetCode);
}
