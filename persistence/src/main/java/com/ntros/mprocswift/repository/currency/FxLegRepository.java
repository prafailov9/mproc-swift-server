package com.ntros.mprocswift.repository.currency;

import com.ntros.mprocswift.model.currency.FxLeg;
import com.ntros.mprocswift.model.currency.FxQuote;
import com.ntros.mprocswift.model.transactions.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FxLegRepository extends JpaRepository<FxLeg, Integer> {

  @Query(value = "SELECT l FROM FxLeg l ORDER BY l.sequence ASC")
  List<FxLeg> findAllOrderBySequence();

  @Query(
      value =
          """
        SELECT l FROM FxLeg l JOIN l.fxQuote q WHERE q.transaction = :transaction ORDER BY l.sequence ASC
        """)
  List<FxLeg> findAllByTransactionOrdered(Transaction transaction);

  @Query(
      value =
          """
                  SELECT l FROM FxLeg l WHERE l.fxQuote = :fxQuote ORDER BY l.sequence ASC
                  """)
  List<FxLeg> findAllByFxQuoteOrdered(FxQuote fxQuote);
}
