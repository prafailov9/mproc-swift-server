package com.ntros.mprocswift.model.transactions.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.PriorityQueue;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "idempotency_record",
    uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class IdempotencyRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idempotency_id")
  private Long idempotencyId;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "request_hash", length = 64)
  private String requestHash;

  @Column(name = "transaction_id")
  private Integer transactionId;

  @Column(name = "status_code")
  private int statusCode;

  @Column(name = "response_body")
  private String responseBody;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private IdempotencyStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;
}
