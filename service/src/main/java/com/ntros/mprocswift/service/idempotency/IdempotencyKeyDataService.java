package com.ntros.mprocswift.service.idempotency;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.transaction.IdempotencyRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdempotencyRecordDataService implements IdempotencyRecordMarkingService  {

  private final IdempotencyRecordRepository idempotencyRecordRepository;

  @Autowired
  public IdempotencyRecordDataService(IdempotencyRecordRepository idempotencyRecordRepository) {
    this.idempotencyRecordRepository = idempotencyRecordRepository;
  }

  @Override
  public void saveRecord(IdempotencyKey idempotencyKey) {
    idempotencyRecordRepository.save(idempotencyKey);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public IdempotencyKey load(String key) {
    return idempotencyRecordRepository
        .findByIdempotencyKey(key)
        .orElseThrow(() -> new NotFoundException("No idempotency row for " + key));
  }

  @Override
  public List<IdempotencyKey> loadAll() {
    return idempotencyRecordRepository.findAll();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean tryClaim(String key, String requestHash) {
    try {
      idempotencyRecordRepository.saveAndFlush(
          new IdempotencyKey(key, requestHash, "IN_PROGRESS"));
      return true;                          // we own it
    } catch (DuplicateKeyException dup) {
      return false;                         // someone already claimed it
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IdempotencyKey load(String key) {
    return idempotencyRecordRepository.findById(key)
            .orElseThrow(() -> new NotFoundException("No idempotency row for " + key));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(String key) {
    idempotencyRecordRepository.findById(key).ifPresent(k -> { k.setStatus("FAILED"); repo.save(k); });
  }

}
