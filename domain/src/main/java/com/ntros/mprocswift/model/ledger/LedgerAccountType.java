package com.ntros.mprocswift.model.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
@Table(name = "ledger_account_types")
public class LedgerAccountType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer ledgerAccountTypeId;

  private String typeCode;
  private String typeDescription;
}
