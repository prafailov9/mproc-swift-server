package com.ntros.mprocswift.service.currency.exchangerate;

import com.ntros.mprocswift.exceptions.ExchangeRateNotFoundForPairException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.currency.*;
import com.ntros.mprocswift.repository.currency.CurrencyExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class CurrencyExchangeRateDataService implements CurrencyExchangeRateService {

  private final CurrencyExchangeRateRepository currencyExchangeRateRepository;

  @Autowired
  public CurrencyExchangeRateDataService(
      CurrencyExchangeRateRepository currencyExchangeRateRepository) {
    this.currencyExchangeRateRepository = currencyExchangeRateRepository;
  }

  @Override
  public CurrencyExchangeRate getExchangeRate(Currency source, Currency target) {
    return currencyExchangeRateRepository
        .findExchangeRateBySourceAndTarget(source, target)
        .orElseThrow(
            () ->
                new ExchangeRateNotFoundForPairException(
                    source.getCurrencyCode(), target.getCurrencyCode()));
  }

  @Override
  public OffsetDateTime getUpdateDateForRate(Currency source, Currency target) {
    return currencyExchangeRateRepository
        .findUpdatedDateForRate(source, target)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "Could not find updatedDate for currencies: source: %s; target: %s",
                        source, target)));
  }
}
