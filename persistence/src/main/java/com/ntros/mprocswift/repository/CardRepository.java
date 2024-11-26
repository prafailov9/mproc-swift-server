package com.ntros.mprocswift.repository;

import com.ntros.mprocswift.model.card.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Integer> {

    @Query(value = "SELECT * FROM card c " +
            "WHERE c.card_provider= :cardProvider " +
            "AND c.card_number= :cardNumber " +
            "AND c.expiration_date= :expirationDate " +
            "AND c.cvv= :cvv", nativeQuery = true)
    Optional<Card> findByNumberExpirationCvv(@Param("cardProvider") String cardProvider,
                                             @Param("cardNumber") String cardNumber,
                                             @Param("expirationDate") String expirationDate,
                                             @Param("cvv") String cvv);

}
