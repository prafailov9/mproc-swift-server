package com.ntros.mprocswift.controller.ledger;

import com.ntros.mprocswift.converter.LedgerAccountBalanceConverter;
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
  private final LedgerAccountBalanceConverter ledgerAccountBalanceConverter;

  @Autowired
  public LedgerAccountBalanceController(
      LedgerAccountBalanceService ledgerAccountBalanceService,
      LedgerAccountBalanceConverter ledgerAccountBalanceConverter) {
    this.ledgerAccountBalanceService = ledgerAccountBalanceService;
    this.ledgerAccountBalanceConverter = ledgerAccountBalanceConverter;
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllBalances() {
    return ResponseEntity.ok(
        ledgerAccountBalanceService.getAllLedgerAccountBalances().stream()
            .map(ledgerAccountBalanceConverter::toDto)
            .toList());
  }

  @GetMapping("/all-held")
  public ResponseEntity<?> getAllHeldBalances() {
    return ResponseEntity.ok(
        ledgerAccountBalanceService.getAllHeldLedgerAccountBalances().stream()
            .map(ledgerAccountBalanceConverter::toDto)
            .toList());
  }

  @GetMapping("/all-avail")
  public ResponseEntity<?> getAllAvailableBalances() {
    return ResponseEntity.ok(
        ledgerAccountBalanceService.getAllAvailableLedgerAccountBalances().stream()
            .map(ledgerAccountBalanceConverter::toDto)
            .toList());
  }

  @GetMapping("/all-sys")
  public ResponseEntity<?> getAllSystemBalances() {
    return ResponseEntity.ok(
        ledgerAccountBalanceService.getAllSystemBalances().stream()
            .map(ledgerAccountBalanceConverter::toDto)
            .toList());
  }
}
