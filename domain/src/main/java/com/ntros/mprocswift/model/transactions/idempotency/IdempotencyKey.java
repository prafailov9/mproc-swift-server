package com.ntros.mprocswift.model.transactions.idempotency;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

/**
 * JPA does a null check on the @Id(idempotencyKey) since is non-primitive, to decide
 * weather to INSERT:persist() or UPDATE:merge() the row. Since the key will always be sent for
 * persisting, to force a constraint error, and with a populated @Id value, JPA will always say
 * the key is NOT NEW and redirect to UPDATE, which breaks the idempotency flow.
 *
 * <p>The @Transient 'isNew' field is a marker, telling JPA to check against it, instead of the
 * default identity check. By default, the field is 'true', but will be marked as 'false' after
 * READ and after INSERT.
 *
 * <p>An alternative to this approach, is to inject an entityManager directly in the Service and
 * just call persist(), instead of relying on JPA. The benefit is keeping this Entity
 * clean.
 */
@Entity
@Data
@Table(name = "idempotency_keys")
public class IdempotencyKey implements Persistable<String> {
  @Id
  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Transient private boolean isNew = true;

  @Column(name = "request_hash", length = 64)
  private String requestHash;

  @Column(name = "transaction_id")
  private Integer transactionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private IdempotencyStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  public IdempotencyKey() {}

  public IdempotencyKey(String key, String hash, IdempotencyStatus status) {
    idempotencyKey = key;
    requestHash = hash;
    this.status = status;
  }

  @Override
  public String getId() {
    return idempotencyKey;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}
