package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.CardPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardPaymentRepository extends JpaRepository<CardPayment, Integer> {
}
