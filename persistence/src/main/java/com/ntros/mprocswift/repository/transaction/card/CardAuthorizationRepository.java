package com.ntros.mprocswift.repository.transaction.card;

import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAuthorizationRepository extends JpaRepository<CardAuthorization, Integer> {
}
