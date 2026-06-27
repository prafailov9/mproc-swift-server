package com.ntros.mprocswift.service.idempotency;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;

import java.util.List;

public interface IdempotencyRecordService {

  void saveRecord(IdempotencyKey idempotencyKey);

  IdempotencyKey load(String key);

  List<IdempotencyKey> loadAll();



}
