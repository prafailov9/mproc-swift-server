package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.exceptions.CurrencyNotSupportedException;
import com.ntros.mprocswift.exceptions.ExchangeRateNotFoundForPairException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.currency.*;
import com.ntros.mprocswift.repository.currency.CurrencyExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ntros.mprocswift.service.currency.CurrencyUtils.BASE_CURRENCIES;

@Service
@Slf4j
public class CurrencyExchangeRateDataService implements CurrencyExchangeRateService {

  private final CurrencyExchangeRateRepository currencyExchangeRateRepository;
  private final CurrencyService currencyService;

  @Autowired
  public CurrencyExchangeRateDataService(
      CurrencyExchangeRateRepository currencyExchangeRateRepository,
      CurrencyService currencyService) {
    this.currencyExchangeRateRepository = currencyExchangeRateRepository;
    this.currencyService = currencyService;
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
  public RatedMoneyMovement convert(long amount, String source, String target) {
    Currency sourceCurrency = currencyService.getCurrencyByCode(source);
    Currency targetCurrency = currencyService.getCurrencyByCode(target);

    // TODO: add money valObj for target amount
    ConvertedAmount converted = convert(amount, sourceCurrency, targetCurrency);

    Money sourceMoney = new Money(amount, sourceCurrency);
    Money targetMoney = new Money(converted.amount(), targetCurrency);

    return new RatedMoneyMovement(
        new MoneyMovement(sourceMoney, targetMoney), converted.appliedRate());
  }

  /**
   * Converts given amount of source currency to target currency. If no direct exchange rate exists
   * for source -> target, will try to find base rate for both or intermediate base rate.
   *
   * @param amount to convert
   * @param source currency
   * @param target currency
   * @return converted amount
   */
  @Override
  public ConvertedAmount convert(long amount, Currency source, Currency target) {
    if (source.getCurrencyCode().equals(target.getCurrencyCode())) {
      log.info("Currencies are the same: {}", source.getCurrencyCode());
      var exchangeRate =
          currencyExchangeRateRepository
              .findExchangeRateBySourceAndTarget(source, target)
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          String.format(
                              "Could not find rate:[%s -> %s]",
                              source.getCurrencyCode(), target.getCurrencyCode())));
      return new ConvertedAmount(amount, exchangeRate);
    }
    return currencyExchangeRateRepository
        .findExchangeRateBySourceAndTarget(source, target)
        .map(
            exchangeRate -> {
              log.info("Found direct exchange rate: {}", exchangeRate);
              return new ConvertedAmount(
                  applyRate(amount, source, target, exchangeRate.getExchangeRate()), exchangeRate);
            })
        .orElseGet(
            () -> {
              log.info(
                  "No direct exchange rate found for {}/{}",
                  source.getCurrencyCode(),
                  target.getCurrencyCode());
              log.info(
                  "searching for base rates: [{}/base -> base/{}]",
                  source.getCurrencyCode(),
                  target.getCurrencyCode());
              return convertWithBase(amount, source, target);
            });
  }

  /**
   * base = USD or EUR find first conversion: [source -> base] find second conversion: [base ->
   * target] check if base currencies for both rates are the same and convert source -> base ->
   * target - ex: A -> USD -> B else: convert to an intermediate base
   *
   * @return converted amount with base rate
   */
  private ConvertedAmount convertWithBase(long amountToConvert, Currency source, Currency target) {
    CurrencyExchangeRate sourceToBase =
        getExchangeRateFromBase(source, true)
            .orElseThrow(() -> new CurrencyNotSupportedException(source.getCurrencyCode()));
    CurrencyExchangeRate baseToTarget =
        getExchangeRateFromBase(target, false)
            .orElseThrow(() -> new CurrencyNotSupportedException(target.getCurrencyCode()));

    long sourceToBaseAmount =
        applyRate(
            amountToConvert,
            sourceToBase.getTargetCurrency(),
            sourceToBase.getTargetCurrency(),
            sourceToBase.getExchangeRate());

    if (sourceToBase
        .getTargetCurrency()
        .getCurrencyCode()
        .equals(baseToTarget.getSourceCurrency().getCurrencyCode())) {
      // convert source to base
      long baseToTargetAmount =
          applyRate(
              sourceToBaseAmount,
              source,
              baseToTarget.getTargetCurrency(),
              baseToTarget.getExchangeRate());
      log.info(
          "Converted {} {} to {} {} with base currency {}",
          amountToConvert,
          source.getCurrencyCode(),
          baseToTargetAmount,
          target.getCurrencyCode(),
          sourceToBase.getTargetCurrency().getCurrencyCode());
      return new ConvertedAmount(baseToTargetAmount, baseToTarget);
    }

    log.info("Found different bases: {} {}", sourceToBase, baseToTarget);
    return convertWithIntermediateBase(sourceToBaseAmount, sourceToBase, baseToTarget);
  }

  /**
   * Convert to intermediate base, if any, and get target amount source -> source's base -> target's
   * base -> target ex: A -> USD -> EUR -> B
   *
   * @param sourceToBaseAmount - first conversion: source -> source's base
   * @param sourceToBase - rate for source to its base
   * @param baseToTarget - rate for target's base to the target
   * @return converted target amount
   */
  private ConvertedAmount convertWithIntermediateBase(
      long sourceToBaseAmount,
      CurrencyExchangeRate sourceToBase,
      CurrencyExchangeRate baseToTarget) {
    CurrencyExchangeRate intermediateBase =
        getExchangeRate(sourceToBase.getTargetCurrency(), baseToTarget.getSourceCurrency());
    long intermediateAmount =
        applyRate(
            sourceToBaseAmount,
            sourceToBase.getTargetCurrency(),
            baseToTarget.getSourceCurrency(),
            intermediateBase.getExchangeRate());
    log.info(
        "Intermediate amount: {} {}",
        intermediateAmount,
        baseToTarget.getSourceCurrency().getCurrencyCode());

    long targetAmount =
        applyRate(
            intermediateAmount,
            baseToTarget.getSourceCurrency(),
            baseToTarget.getTargetCurrency(),
            baseToTarget.getExchangeRate());
    log.info("Converted amount: {}", targetAmount);

    return new ConvertedAmount(targetAmount, baseToTarget);
  }

  private Optional<CurrencyExchangeRate> getExchangeRateFromBase(
      Currency currencyToConvert, boolean isBase) {
    return BASE_CURRENCIES.stream()
        .map(
            baseCurrencyCode ->
                getRate(currencyToConvert.getCurrencyCode(), baseCurrencyCode, isBase))
        .flatMap(Optional::stream) // filters empty Optionals, unwraps values of non-empty ones
        .findFirst();
  }

  /** if direction is true -> toConvert/base else -> base/toConvert */
  private Optional<CurrencyExchangeRate> getRate(String toConvert, String base, boolean direction) {
    return direction
        ? currencyExchangeRateRepository.findExchangeRateBySourceCodeAndTargetCode(toConvert, base)
        : currencyExchangeRateRepository.findExchangeRateBySourceCodeAndTargetCode(base, toConvert);
  }

  private long applyRate(long sourceMinor, Currency source, Currency target, BigDecimal rate) {
    return BigDecimal.valueOf(sourceMinor)
        .movePointLeft(source.getExponent()) // minor -> major (source)
        .multiply(rate) // major source -> major target
        .movePointRight(target.getExponent()) // major -> minor (target)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
