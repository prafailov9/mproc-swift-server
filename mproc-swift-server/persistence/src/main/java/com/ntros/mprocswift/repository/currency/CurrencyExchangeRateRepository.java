package com.ntros.mprocswift.repository.currency;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.CurrencyExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CurrencyExchangeRateRepository extends JpaRepository<CurrencyExchangeRate, Integer> {

    @Query(value = "SELECT r FROM CurrencyExchangeRate r WHERE r.sourceCurrency = :source AND r.targetCurrency = :target")
    Optional<CurrencyExchangeRate> findExchangeRateBySourceAndTarget(@Param("source") Currency source, @Param("target") Currency target);

    @Query(value = "SELECT r.exchangeRate FROM CurrencyExchangeRate r WHERE r.sourceCurrency = :source AND r.targetCurrency = :target")
    Optional<BigDecimal> findExchangeRateValueBySourceAndTarget(@Param("source") Currency source, @Param("target") Currency target);


    @Query(value = """
            SELECT r.* FROM currency_exchange_rate r
            JOIN currency sc ON sc.currency_id=r.source_currency_id
            JOIN currency tc ON tc.currency_id=r.target_currency_id
            WHERE sc.currency_code= :sourceCode AND tc.currency_code= :targetCode""", nativeQuery = true)
    Optional<CurrencyExchangeRate> findExchangeRateBySourceCodeAndTargetCode(@Param("sourceCode") String sourceCode, @Param("targetCode") String targetCode);

}
