package com.ntros.mprocswift.model.currency;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Unique currency conversion step, explaining a single movement from an inputAmount to outputAmount
 * with the [inputCurrency->outputCurrency] exchange appliedRate applied. Part of the Audit trail(FxQuote)
 * for currency conversions.
 */
@Entity
@Data
@RequiredArgsConstructor
@Table(name = "fx_legs")
public class FxLeg {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Integer fxLegId;

  @ManyToOne(cascade = CascadeType.REMOVE)
  @JoinColumn(name = "fx_quote_id")
  private FxQuote fxQuote;

  @ManyToOne
  @JoinColumn(name = "input_currency_id")
  private Currency inputCurrency;

  @ManyToOne
  @JoinColumn(name = "output_currency_id")
  private Currency outputCurrency;

  /** The position in which the leg appears in the trails */
  @Column(name = "seq", nullable = false)
  private int sequence;

  /** Rate, applied on the input to produce the output */
  @Column(name = "applied_rate", nullable = false)
  private BigDecimal appliedRate;

  private long inputAmount;
  private long outputAmount;

  private void incrementSequence() {
    sequence++;
  }
}
