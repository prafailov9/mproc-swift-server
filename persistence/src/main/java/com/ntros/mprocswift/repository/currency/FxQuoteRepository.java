package com.ntros.mprocswift.repository.currency;

import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FxQuoteRepository extends JpaRepository<FxQuote, Integer> {

  @Query(value = "SELECT q FROM FxQuote q WHERE q.transaction = :transaction")
  Optional<FxQuote> findByTransaction(Transaction transaction);

  @Query(
      value =
          """
        Select q FROM FxQuote q JOIN q.sourceCurrency src JOIN q.targetCurrency tar
                WHERE src = :source AND tar = :target
        """)
  List<FxQuote> findAllBySourceTargetCurrencies(Currency source, Currency target);

  @Query(
      value =
          """
                  Select q FROM FxQuote q JOIN q.sourceCurrency src JOIN q.targetCurrency tar
                          WHERE src.currencyCode = :source AND tar.currencyCode = :target
                  """)
  List<FxQuote> findAllBySourceTargetCurrencyCodes(
      @Param("source") String source, @Param("target") String target);
}
