package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {

    Optional<TransactionType> findByTypeName(@Param("typeName") String typeName);

}
