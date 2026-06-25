package com.ntros.mprocswift.service.idempotency;

public interface IdempotencyRecordMarkingService extends IdempotencyRecordService {
  boolean tryClaim(String key, String requestHash);

  void markFailed(String key);

  void markCompleted(String key, Integer transactionId);
}
