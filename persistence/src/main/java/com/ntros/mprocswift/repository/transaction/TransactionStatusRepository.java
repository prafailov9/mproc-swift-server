package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {

    Optional<TransactionStatus> findByStatusName(@Param("statusName") String statusName);

}
