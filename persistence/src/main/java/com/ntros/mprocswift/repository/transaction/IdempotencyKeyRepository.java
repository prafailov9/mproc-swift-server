package com.ntros.mprocswift.repository.transaction;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

  Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
