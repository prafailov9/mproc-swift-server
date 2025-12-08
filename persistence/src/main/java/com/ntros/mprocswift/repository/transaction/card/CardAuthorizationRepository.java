package com.ntros.mprocswift.repository.transaction.card;

import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardAuthorizationRepository extends JpaRepository<CardAuthorization, Integer> {

    Optional<CardAuthorization> findOneByAuthorizationCode(@Param("authorizationCode") String authorizationCode);

}
