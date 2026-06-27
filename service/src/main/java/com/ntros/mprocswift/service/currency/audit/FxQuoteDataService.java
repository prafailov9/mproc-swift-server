package com.ntros.mprocswift.service.currency.audit;

import com.ntros.mprocswift.exceptions.EntityCreateFailedException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.repository.currency.FxQuoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FxQuoteDataService implements FxQuoteService {

  private final FxQuoteRepository fxQuoteRepository;

  public FxQuoteDataService(FxQuoteRepository fxQuoteRepository) {
    this.fxQuoteRepository = fxQuoteRepository;
  }

  @Override
  public FxQuote getQuoteByTransaction(Transaction transaction) {
    return fxQuoteRepository
        .findByTransaction(transaction)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Could not fix FX_Quote for transaction %s", transaction)));
  }

  @Override
  public FxQuote createFxQuote(FxQuote fxQuote) {
    try {
      FxQuote saved = fxQuoteRepository.save(fxQuote);
      log.info("FX_Quote saved. {}", saved);
      return saved;
    } catch (DataIntegrityViolationException ex) {
      String err =
          String.format("Failed to create FxQuote: %s. Error: %s", fxQuote, ex.getMessage());
      log.error(err);
      throw new EntityCreateFailedException(err, ex);
    }
  }

  @Override
  public List<FxQuote> getAllQuotesForCurrencies(Currency source, Currency target) {
    return fxQuoteRepository.findAllBySourceTargetCurrencies(source, target);
  }

  @Override
  public List<FxQuote> getAllQuotesForCurrencyCodes(String sourceCode, String targetCode) {
    return fxQuoteRepository.findAllBySourceTargetCurrencyCodes(sourceCode, targetCode);
  }
}
