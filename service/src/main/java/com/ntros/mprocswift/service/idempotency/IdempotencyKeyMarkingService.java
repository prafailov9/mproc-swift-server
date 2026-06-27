package com.ntros.mprocswift.service.idempotency;

public interface IdempotencyKeyMarkingService extends IdempotencyKeyService {

  void claim(String key, String requestHash);
  boolean tryClaim(String key, String requestHash);

  void markFailed(String key);

  void markCompleted(String key, Integer transactionId);
}
