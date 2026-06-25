package com.ntros.mprocswift.service.idempotency;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyRecord;

import java.util.List;

public interface IdempotencyRecordService {

  void saveRecord(IdempotencyRecord idempotencyRecord);

  IdempotencyRecord load(String key);

  List<IdempotencyRecord> loadAll();



}
