package com.ntros.mprocswift.model.currency;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
@EqualsAndHashCode
@Table(name = "currencies")
public class Currency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer currencyId;

  @Column(name = "minor_units", nullable = false)
  private int exponent;

  @Column(nullable = false)
  private String currencyCode;

  @Column(nullable = false)
  private String currencyName;

  private boolean isActive;
}
