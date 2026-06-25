package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

  Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
