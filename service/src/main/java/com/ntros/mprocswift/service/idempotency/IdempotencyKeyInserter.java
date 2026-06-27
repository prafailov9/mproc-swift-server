package com.ntros.mprocswift.service.idempotency;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;

public interface IdempotencyKeySaver {
  void insert(String key, String hash);

  IdempotencyKey insert(IdempotencyKey idempotencyKey);
}
