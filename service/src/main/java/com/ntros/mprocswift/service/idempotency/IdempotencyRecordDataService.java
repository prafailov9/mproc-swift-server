package com.ntros.mprocswift.service.idempotency;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyRecord;
import com.ntros.mprocswift.repository.transaction.IdempotencyRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdempotencyRecordDataService implements IdempotencyRecordService {

  private final IdempotencyRecordRepository idempotencyRecordRepository;

  @Autowired
  public IdempotencyRecordDataService(IdempotencyRecordRepository idempotencyRecordRepository) {
    this.idempotencyRecordRepository = idempotencyRecordRepository;
  }

  @Override
  public void saveRecord(IdempotencyRecord idempotencyRecord) {
    idempotencyRecordRepository.save(idempotencyRecord);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public IdempotencyRecord load(String key) {
    return idempotencyRecordRepository
        .findByIdempotencyKey(key)
        .orElseThrow(() -> new NotFoundException("No idempotency row for " + key));
  }

  @Override
  public List<IdempotencyRecord> loadAll() {
    return idempotencyRecordRepository.findAll();
  }

  //  @Override
  //  @Transactional(propagation = Propagation.REQUIRES_NEW)
  //  public boolean tryClaim(String key, String requestHash) {
  //    try {
  //      idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(key, requestHash,
  // "IN_PROGRESS"));
  //      return true;
  //    } catch (DataIntegrityViolationException ex) {
  //      return false;
  //    }
  //  }
  //
  //
  //  @Override
  //  public List<IdempotencyRecord> loadAll() {
  //    return idempotencyRecordRepository.findAll();
  //  }
  //
  //  @Transactional(propagation = Propagation.REQUIRES_NEW)
  //  @Override
  //  public void markFailed(String key) {
  //    idempotencyRecordRepository
  //        .findById(key)
  //        .ifPresent(
  //            k -> {
  //              k.setStatus("FAILED");
  //              idempotencyRecordRepository.save(k);
  //            });
  //  }
  //
  //  @Transactional   // default REQUIRED — joins the transfer's transaction
  //  @Override
  //  public void markCompleted(String key, Integer transactionId) {     // add to the interface too
  //    IdempotencyRecord k = idempotencyRecordRepository.findById(key)
  //            .orElseThrow(() -> new NotFoundException("No idempotency row for " + key));
  //    k.setStatus("COMPLETED");
  //    k.setTransactionId(transactionId);
  //    idempotencyRecordRepository.save(k);
  //  }

}
