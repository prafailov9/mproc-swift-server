package com.ntros.mprocswift.repository.transaction.card;

import com.ntros.mprocswift.model.transactions.card.HoldSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoldSettlementRepository extends JpaRepository<HoldSettlement, Integer> {
}
