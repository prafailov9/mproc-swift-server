package com.ntros.mprocswift.service.idempotency;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.*;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus;
import com.ntros.mprocswift.repository.transaction.IdempotencyKeyRepository;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class IdempotencyKeyDataService implements IdempotencyKeyMarkingService {

  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final IdempotencyKeyInserter idempotencyKeyInserter;

  @Autowired
  public IdempotencyKeyDataService(
      IdempotencyKeyRepository idempotencyKeyRepository, IdempotencyKeyInserter idempotencyKeyInserter) {
    this.idempotencyKeyRepository = idempotencyKeyRepository;
    this.idempotencyKeyInserter = idempotencyKeyInserter;
  }

  @Override
  public void saveKey(IdempotencyKey idempotencyKey) {
    idempotencyKeyRepository.save(idempotencyKey);
  }

  @Override
  public List<IdempotencyKey> loadAll() {
    return idempotencyKeyRepository.findAll();
  }

  @Override
  public void deleteKey(String key) {
    idempotencyKeyRepository.deleteById(key);
  }

  @Override
  public void deleteAll() {
    idempotencyKeyRepository.deleteAll();
  }

  @Override
  public void claim(String key, String requestHash) {
    idempotencyKeyInserter.insert(key, requestHash);
  }

  public boolean tryClaim(String key, String requestHash) {
    var idempotencyKey = new IdempotencyKey(key, requestHash, IN_PROGRESS);
    try {
      var savedKey = idempotencyKeyInserter.insert(idempotencyKey);
      log.info("Key saved: {}", savedKey);
      return true; // key not exist
    } catch (DataIntegrityViolationException ex) {
      log.error("Could not save idempotency key: {}. Error: {}", idempotencyKey, ex.getMessage());
      return false; // key exist
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IdempotencyKey load(String key) {
    return idempotencyKeyRepository
        .findById(key)
        .orElseThrow(() -> new NotFoundException("No idempotency row for " + key));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(String key) {
    mark(key, FAILED, null);
  }

  @Transactional // default REQUIRED -> joins the parent transaction
  @Override
  public void markCompleted(String key, Integer transactionId) {
    mark(key, COMPLETED, transactionId);
  }

  private void mark(String key, IdempotencyStatus status, Integer txnId) {
    var keyOpt = idempotencyKeyRepository.findById(key);
    if (keyOpt.isPresent()) {
      var k = keyOpt.get();
      k.setStatus(status);
      if (txnId != null) {
        k.setTransactionId(txnId);
      }
      var saved = idempotencyKeyRepository.save(k);
      log.info("Idempotency key {} marked {} in db.", saved, status);
    }
    log.info("Idempotency key {} not found.", key);
  }
}
