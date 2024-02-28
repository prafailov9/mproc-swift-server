package com.ntros.mprocswift.repository.currency;

import com.ntros.mprocswift.model.currency.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

    @Query(value = "SELECT c FROM Currency c WHERE c.currencyName= :currencyName")
    Optional<Currency> findByCurrencyName(@Param("currencyName") final String currencyName);

    @Query(value = "SELECT c FROM Currency c WHERE c.currencyCode= :currencyCode")
    Optional<Currency> findByCurrencyCode(@Param("currencyCode") final String currencyCode);

    @Query(value = "SELECT c FROM Currency c WHERE c.currencyCode= :currencyCode AND isActive = true")
    Optional<Currency> findByCurrencyCodeActive(@Param("currencyCode") final String currencyCode);

    @Query(value = "UPDATE Currency SET isActive = true")
    Boolean updateActivateAll();

}
