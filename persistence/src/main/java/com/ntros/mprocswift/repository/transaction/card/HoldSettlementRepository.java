package com.ntros.mprocswift.repository.transaction.card;

import com.ntros.mprocswift.model.transactions.card.HoldSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoldSettlementRepository extends JpaRepository<HoldSettlement, Integer> {

    @Query("SELECT s FROM HoldSettlement s " +
            "JOIN s.cardAuthorization ca " +
            "WHERE ca.authorizationCode = :authorizationCode")
    Optional<HoldSettlement> findOneByAuthCode(@Param("authorizationCode") String authorizationCode);

}
