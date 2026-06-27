package com.ntros.mprocswift.model.currency;

import com.ntros.mprocswift.model.transactions.Transaction;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Full audit log on a single currency conversion run */
@Entity
@Data
@RequiredArgsConstructor
public class FxQuote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "fx_quote_id")
  public Integer fxQuoteId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id", nullable = false, unique = true)
  private Transaction transaction;

  @ManyToOne
  @JoinColumn(name = "source_currency_id")
  public Currency sourceCurrency;

  @ManyToOne
  @JoinColumn(name = "target_currency_id")
  public Currency targetCurrency;

  public long sourceAmount;
  public long targetAmount;

  /**
   * Final effective appliedRate applied on the initial inputCurrency to produce the final outputCurrency, to be
   * debited/credited.
   */
  @Column(name = "final_rate", nullable = false)
  public BigDecimal finalRate;

  @OneToMany(mappedBy = "fxQuote", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("seq ASC")
  private List<FxLeg> legs = new ArrayList<>();
}
