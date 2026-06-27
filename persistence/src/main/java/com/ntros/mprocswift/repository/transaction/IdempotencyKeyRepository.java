package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyKey, Long> {

  Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
