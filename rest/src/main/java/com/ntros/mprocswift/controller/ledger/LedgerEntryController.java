package com.ntros.mprocswift.controller.ledger;

import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/ledger-entries")
public class LedgerEntryController {

  private final LedgerEntryService ledgerEntryService;

  @Autowired
  public LedgerEntryController(LedgerEntryService ledgerEntryService) {
    this.ledgerEntryService = ledgerEntryService;
  }

  @GetMapping("/all")
  public ResponseEntity<?> getAllLedgerEntries() {
    return ResponseEntity.ok(ledgerEntryService.getAllEntries());
  }
}
