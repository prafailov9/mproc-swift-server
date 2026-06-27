package com.ntros.mprocswift.service.currency.exchangerate;

import static com.ntros.mprocswift.service.currency.CurrencyUtils.BASE_CURRENCIES;

import com.ntros.mprocswift.exceptions.CurrencyNotSupportedException;
import com.ntros.mprocswift.exceptions.ExchangeRateNotFoundForPairException;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;
import com.ntros.mprocswift.model.currency.Money;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionLeg;
import com.ntros.mprocswift.dto.quotes.conversion.ConversionQuote;
import com.ntros.mprocswift.repository.currency.CurrencyExchangeRateRepository;
import com.ntros.mprocswift.service.currency.CurrencyService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FxRateConversionService implements FxConversionService {

  private final CurrencyService currencyService;
  private final CurrencyExchangeRateRepository currencyExchangeRateRepository;

  @Autowired
  public FxRateConversionService(
      CurrencyService currencyService,
      CurrencyExchangeRateRepository currencyExchangeRateRepository) {
    this.currencyService = currencyService;
    this.currencyExchangeRateRepository = currencyExchangeRateRepository;
  }

  @Override
  public ConversionQuote convert(
      long amount, String sourceCurrencyCode, String targetCurrencyCode) {

    Currency sourceCurrency = currencyService.getCurrencyByCode(sourceCurrencyCode);
    Currency targetCurrency = currencyService.getCurrencyByCode(targetCurrencyCode);

    return convert(amount, sourceCurrency, targetCurrency);
  }

  /**
   * Converts given amount of inputCurrency currency to outputCurrency currency. If no direct
   * exchange appliedRate exists for inputCurrency -> outputCurrency, will try to find base appliedRate for both
   * or intermediate base appliedRate.
   *
   * @param amount to convert
   * @param sourceCurrency currency
   * @param targetCurrency currency
   * @return converted amount
   */
  @Override
  public ConversionQuote convert(long amount, Currency sourceCurrency, Currency targetCurrency) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Conversion amount must be > 0.");
    }
    // 1. equal currencies, single leg quote
    return directConversion(amount, sourceCurrency, targetCurrency)
        .map(
            leg -> {
              Money sourceMoney = new Money(leg.inputAmount(), sourceCurrency);
              Money targetMoney = new Money(leg.outputAmount(), targetCurrency);
              return ConversionQuote.singleLegQuote(
                  sourceMoney, targetMoney, computeEffectiveRate(sourceMoney, targetMoney));
            })
        .orElseGet(
            () -> {
              log.info(
                  "No direct exchange appliedRate found for {}/{}",
                  sourceCurrency.getCurrencyCode(),
                  targetCurrency.getCurrencyCode());
              log.info(
                  "searching for base rates: [{}/base -> base/{}]",
                  sourceCurrency.getCurrencyCode(),
                  targetCurrency.getCurrencyCode());
              List<ConversionLeg> legs = convertWithBase(amount, sourceCurrency, targetCurrency);
              Money sourceMoney = new Money(amount, sourceCurrency);
              Money targetMoney = new Money(legs.getLast().outputAmount(), targetCurrency);
              var effectiveRateApplied = computeEffectiveRate(sourceMoney, targetMoney);
              return new ConversionQuote(
                  sourceMoney, targetMoney, effectiveRateApplied, List.copyOf(legs));
            });
  }

  private Optional<ConversionLeg> directConversion(
      long amount, Currency sourceCurrency, Currency targetCurrency) {
    if (sourceCurrency.getCurrencyCode().equals(targetCurrency.getCurrencyCode())) {
      log.info("Currencies are the same: {}", sourceCurrency.getCurrencyCode());
      // 1. same-value appliedRate(1.0), single-leg quote
      return Optional.of(
          new ConversionLeg(sourceCurrency, targetCurrency, BigDecimal.ONE, amount, amount));
    }

    // 2. currencies differ, attempt direct conversion
    var exchangeRate =
        currencyExchangeRateRepository.findExchangeRateBySourceAndTarget(
            sourceCurrency, targetCurrency);
    if (exchangeRate.isPresent()) {
      var rate = exchangeRate.get();
      log.info("Found direct exchange appliedRate: {}", rate);
      var convertedAmount =
          applyRate(amount, sourceCurrency, targetCurrency, rate.getExchangeRate());
      return Optional.of(
          new ConversionLeg(
              sourceCurrency, targetCurrency, rate.getExchangeRate(), amount, convertedAmount));
    }
    return Optional.empty();
  }

  /**
   * base = USD or EUR find first conversion: [inputCurrency -> base] find second conversion: [base
   * -> outputCurrency] check if base currencies for both rates are the same and convert
   * inputCurrency -> base -> outputCurrency - ex: A -> USD -> B else: convert to an intermediate
   * base
   *
   * @return converted amount with base appliedRate
   */
  private List<ConversionLeg> convertWithBase(
      long amountToConvert, Currency source, Currency target) {
    // 2. two-legged quote; src-> base, base-> trg
    // collect leg trail
    List<ConversionLeg> legs = new ArrayList<>();

    CurrencyExchangeRate sourceToBase =
        getExchangeRateFromBase(source, true)
            .orElseThrow(() -> new CurrencyNotSupportedException(source.getCurrencyCode()));
    CurrencyExchangeRate baseToTarget =
        getExchangeRateFromBase(target, false)
            .orElseThrow(() -> new CurrencyNotSupportedException(target.getCurrencyCode()));

    long sourceToBaseAmount =
        applyRate(
            amountToConvert,
            sourceToBase.getSourceCurrency(),
            sourceToBase.getTargetCurrency(),
            sourceToBase.getExchangeRate());

    // create first leg
    legs.add(ConversionLeg.fromExchangeRate(sourceToBase, amountToConvert, sourceToBaseAmount));

    if (sourceToBase
        .getTargetCurrency()
        .getCurrencyCode()
        .equals(baseToTarget.getSourceCurrency().getCurrencyCode())) {
      // convert inputCurrency to base
      long baseToTargetAmount =
          applyRate(
              sourceToBaseAmount,
              baseToTarget.getSourceCurrency(),
              baseToTarget.getTargetCurrency(),
              baseToTarget.getExchangeRate());
      log.info(
          "Converted {} {} to {} {} with base currency {}",
          amountToConvert,
          source.getCurrencyCode(),
          baseToTargetAmount,
          target.getCurrencyCode(),
          sourceToBase.getTargetCurrency().getCurrencyCode());
      // collect second leg
      legs.add(
          ConversionLeg.fromExchangeRate(baseToTarget, sourceToBaseAmount, baseToTargetAmount));
      return List.copyOf(legs);
    }

    log.info("Found different bases: {} {}", sourceToBase, baseToTarget);
    return convertWithIntermediateBase(sourceToBaseAmount, sourceToBase, baseToTarget, legs);
  }

  /**
   * Convert to intermediate base, if any, and get outputCurrency amount inputCurrency ->
   * inputCurrency's base -> outputCurrency's base -> outputCurrency ex: A -> USD -> EUR -> B
   *
   * @param sourceToBaseAmount - first conversion: inputCurrency -> inputCurrency's base
   * @param sourceToBase - appliedRate for inputCurrency to its base
   * @param baseToTarget - appliedRate for outputCurrency's base to the outputCurrency
   * @return converted outputCurrency amount
   */
  private List<ConversionLeg> convertWithIntermediateBase(
      long sourceToBaseAmount,
      CurrencyExchangeRate sourceToBase,
      CurrencyExchangeRate baseToTarget,
      List<ConversionLeg> legs) {
    // intermediate step: from base1 to base2, if possible, then base2 -> outputCurrency
    CurrencyExchangeRate intermediateBase =
        currencyExchangeRateRepository
            .findExchangeRateBySourceAndTarget(
                sourceToBase.getTargetCurrency(), baseToTarget.getSourceCurrency())
            .orElseThrow(
                () ->
                    new ExchangeRateNotFoundForPairException(
                        sourceToBase.getTargetCurrency().getCurrencyCode(),
                        baseToTarget.getSourceCurrency().getCurrencyCode()));

    // apply base1 -> base2 and collect
    long intermediateAmount =
        applyRate(
            sourceToBaseAmount,
            sourceToBase.getTargetCurrency(),
            baseToTarget.getSourceCurrency(),
            intermediateBase.getExchangeRate());

    legs.add(
        new ConversionLeg(
            sourceToBase.getTargetCurrency(),
            baseToTarget.getSourceCurrency(),
            intermediateBase.getExchangeRate(),
            sourceToBaseAmount,
            intermediateAmount));

    log.info(
        "Intermediate amount: {} {}",
        intermediateAmount,
        baseToTarget.getSourceCurrency().getCurrencyCode());

    // apply base2 -> outputCurrency
    long targetAmount =
        applyRate(
            intermediateAmount,
            baseToTarget.getSourceCurrency(),
            baseToTarget.getTargetCurrency(),
            baseToTarget.getExchangeRate());
    log.info("Converted amount: {}", targetAmount);
    legs.add(ConversionLeg.fromExchangeRate(baseToTarget, intermediateAmount, targetAmount));
    return legs;
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

  private long applyRate(long sourceAmount, Currency source, Currency target, BigDecimal rate) {
    return BigDecimal.valueOf(sourceAmount)
        .movePointLeft(source.getExponent()) // minor -> major (inputCurrency)
        .multiply(rate) // major inputCurrency -> major outputCurrency
        .movePointRight(target.getExponent()) // major -> minor (outputCurrency)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private BigDecimal computeEffectiveRate(Money sourceMoney, Money targetMoney) {
    BigDecimal sourceMajor =
        BigDecimal.valueOf(sourceMoney.minorAmount())
            .movePointLeft(sourceMoney.currency().getExponent());

    BigDecimal targetMajor =
        BigDecimal.valueOf(targetMoney.minorAmount())
            .movePointLeft(targetMoney.currency().getExponent());

    return targetMajor.divide(sourceMajor, 18, RoundingMode.HALF_UP);
  }
}
