package com.ntros.mprocswift.controller.ledger;

import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import com.ntros.mprocswift.repository.ledger.LedgerAccountBalanceRepository;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/balances")
public class LedgerAccountBalanceController {

  private final LedgerAccountBalanceService ledgerAccountBalanceService;

  @Autowired
  public LedgerAccountBalanceController(LedgerAccountBalanceService ledgerAccountBalanceService) {
    this.ledgerAccountBalanceService = ledgerAccountBalanceService;
  }

  @GetMapping("/all")
  public ResponseEntity<List<LedgerAccountBalance>> getAllBalances() {
    return null;
  }
}
