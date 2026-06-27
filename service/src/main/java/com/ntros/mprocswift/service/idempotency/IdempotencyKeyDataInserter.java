package com.ntros.mprocswift.service.idempotency;

import static com.ntros.mprocswift.model.transactions.idempotency.IdempotencyStatus.IN_PROGRESS;

import com.ntros.mprocswift.model.transactions.idempotency.IdempotencyKey;
import com.ntros.mprocswift.repository.transaction.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyKeyDataSaver implements IdempotencyKeySaver {

  private final IdempotencyKeyRepository idempotencyKeyRepository;

  @Autowired
  public IdempotencyKeyDataSaver(IdempotencyKeyRepository idempotencyKeyRepository) {
    this.idempotencyKeyRepository = idempotencyKeyRepository;
  }

  // transaction in a different bean so it does not pollute the caller bean's tx
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public void insert(String key, String hash) {
    idempotencyKeyRepository.saveAndFlush(new IdempotencyKey(key, hash, IN_PROGRESS));
  }

  @Override
  public IdempotencyKey insert(IdempotencyKey idempotencyKey) {
    return idempotencyKeyRepository.saveAndFlush(idempotencyKey);
  }
}
