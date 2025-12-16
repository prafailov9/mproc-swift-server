package com.ntros.mprocswift.model.transactions;

import com.ntros.mprocswift.model.currency.Currency;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer transactionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_transaction_id")
  private Transaction relatedTransaction;

  @ManyToOne
  @JoinColumn(name = "transaction_type_id")
  private TransactionType type;

  @ManyToOne
  @JoinColumn(name = "transaction_status_id")
  private TransactionStatus status;

  @ManyToOne
  @JoinColumn(name = "currency_id")
  private Currency currency;

  private long amount;
  private long fees;

  private OffsetDateTime transactionDate;
  private String description;
}
