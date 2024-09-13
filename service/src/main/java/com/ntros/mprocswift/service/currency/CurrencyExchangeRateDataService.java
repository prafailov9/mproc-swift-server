package com.ntros.mprocswift.service.currency;

import com.ntros.mprocswift.exceptions.CurrencyNotSupportedException;
import com.ntros.mprocswift.exceptions.ExchangeRateNotFoundForPairException;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;
import com.ntros.mprocswift.repository.currency.CurrencyExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.ntros.mprocswift.service.currency.CurrencyUtils.getScale;

@Service
@Slf4j
public class CurrencyExchangeRateDataService implements CurrencyExchangeRateService {


    private final CurrencyExchangeRateRepository currencyExchangeRateRepository;
    private final CurrencyService currencyService;

    @Autowired
    public CurrencyExchangeRateDataService(CurrencyExchangeRateRepository currencyExchangeRateRepository, CurrencyService currencyService) {
        this.currencyExchangeRateRepository = currencyExchangeRateRepository;
        this.currencyService = currencyService;
    }

    @Override
    public CurrencyExchangeRate getExchangeRate(Currency source, Currency target) {
        return currencyExchangeRateRepository
                .findExchangeRateBySourceAndTarget(source, target)
                .orElseThrow(() -> new ExchangeRateNotFoundForPairException(source.getCurrencyCode(), target.getCurrencyCode()));
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String source, String target) {
        Currency sourceCurrency = currencyService.getCurrencyByCode(source);
        Currency targetCurrency = currencyService.getCurrencyByCode(target);
        return convert(amount, sourceCurrency, targetCurrency);
    }

    /**
     * Converts given amount of source currency to target currency.
     * If no direct exchange rate exists for source -> target,
     * will try to find base rate for both or intermediate base rate.
     *
     * @param amount to convert
     * @param source currency
     * @param target currency
     * @return converted amount
     */
    @Override
    public BigDecimal convert(final BigDecimal amount, Currency source, Currency target) {
        return currencyExchangeRateRepository.findExchangeRateValueBySourceAndTarget(source, target)
                .map(rate -> {
                    log.info("Found direct exchange rate: {}", rate);
                    return amount.multiply(rate.setScale(getScale(rate), RoundingMode.HALF_UP));
                })
                .orElseGet(() -> {
                    log.info("No direct exchange rate found for {}/{}", source.getCurrencyCode(), target.getCurrencyCode());
                    log.info("searching for base rates: [{}/base -> base/{}]", source.getCurrencyCode(), target.getCurrencyCode());
                    return convertWithBase(amount, source, target);
                });
    }

    /**
     * base = USD or EUR
     * find first conversion: [source -> base]
     * find second conversion: [base -> target]
     * check if base currencies for both rates are the same
     * and convert source -> base -> target
     * - ex: A -> USD -> B
     * else: convert to an intermediate base
     *
     * @return converted amount with base rate
     */
    private BigDecimal convertWithBase(BigDecimal amountToConvert, Currency source, Currency target) {
        CurrencyExchangeRate sourceToBase = getExchangeRateForBase(source, true);
        CurrencyExchangeRate baseToTarget = getExchangeRateForBase(target, false);
        BigDecimal sourceToBaseAmount = amountToConvert.divide(sourceToBase.getExchangeRate(),
                getScale(amountToConvert), RoundingMode.HALF_UP);

        if (sourceToBase.getTargetCurrency().getCurrencyCode().equals(baseToTarget.getSourceCurrency().getCurrencyCode())) {
            // convert source to base
            BigDecimal baseToTargetAmount = sourceToBaseAmount.multiply(baseToTarget.getExchangeRate());
            log.info("Converted {} {} to {} {} with base currency {}", amountToConvert, source.getCurrencyCode(),
                    baseToTargetAmount, target.getCurrencyCode(), sourceToBase.getTargetCurrency().getCurrencyCode());
            return baseToTargetAmount.setScale(getScale(baseToTargetAmount), RoundingMode.HALF_UP);
        }

        log.info("Found different bases: {} {}", sourceToBase, baseToTarget);
        return convertWithIntermediateBase(sourceToBaseAmount, sourceToBase, baseToTarget);
    }

    /**
     * Convert to intermediate base, if any, and get target amount
     * source -> source's base -> target's base -> target
     * ex: A -> USD -> EUR -> B
     *
     * @param sourceToBaseAmount - first conversion: source -> source's base
     * @param sourceToBase       - rate for source to its base
     * @param baseToTarget       - rate for target's base to the target
     * @return converted target amount
     */
    private BigDecimal convertWithIntermediateBase(BigDecimal sourceToBaseAmount, CurrencyExchangeRate sourceToBase, CurrencyExchangeRate baseToTarget) {
        CurrencyExchangeRate intermediateBase = getExchangeRate(sourceToBase.getTargetCurrency(), baseToTarget.getSourceCurrency());
        BigDecimal intermediateAmount = sourceToBaseAmount.multiply(intermediateBase.getExchangeRate());
        log.info("Intermediate amount: {} {}", intermediateAmount, baseToTarget.getSourceCurrency().getCurrencyCode());

        BigDecimal targetAmount = intermediateAmount.multiply(baseToTarget.getExchangeRate()
                .setScale(getScale(baseToTarget.getExchangeRate()), RoundingMode.HALF_UP));
        log.info("Converted amount: {}", targetAmount);

        return targetAmount;
    }

    private CurrencyExchangeRate getExchangeRateForBase(Currency currencyToConvert, boolean isBase) {
        return CurrencyUtils.BASE_CURRENCIES
                .stream()
                .map(baseCurrencyCode -> getRate(currencyToConvert.getCurrencyCode(), baseCurrencyCode, isBase))
                .flatMap(Optional::stream) // filters empty Optionals, unwraps values of non-empty ones
                .findFirst()
                .orElseThrow(() -> new CurrencyNotSupportedException(currencyToConvert.getCurrencyCode()));
    }

    /**
     * if direction is true -> toConvert/base
     * else -> base/toConvert
     */
    private Optional<CurrencyExchangeRate> getRate(String toConvert, String base, boolean direction) {
        return direction
                ? currencyExchangeRateRepository.findExchangeRateBySourceCodeAndTargetCode(toConvert, base)
                : currencyExchangeRateRepository.findExchangeRateBySourceCodeAndTargetCode(base, toConvert);
    }
}
