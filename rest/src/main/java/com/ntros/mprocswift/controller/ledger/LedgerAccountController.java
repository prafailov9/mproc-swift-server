package com.ntros.mprocswift.controller.ledger;

import com.ntros.mprocswift.controller.AbstractApiController;
import com.ntros.mprocswift.converter.LedgerAccountConverter;
import com.ntros.mprocswift.dto.ledger.LedgerAccountDTO;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/ledgerAccount")
public class LedgerAccountController extends AbstractApiController {

  private final LedgerAccountService ledgerAccountService;
  private final LedgerAccountConverter ledgerAccountConverter;

  @Autowired
  public LedgerAccountController(
      LedgerAccountService ledgerAccountService, LedgerAccountConverter ledgerAccountConverter) {
    this.ledgerAccountService = ledgerAccountService;
    this.ledgerAccountConverter = ledgerAccountConverter;
  }

  @GetMapping("/all")
  public ResponseEntity<Set<LedgerAccountDTO>> getAllLedgerAccounts() {
    return ResponseEntity.ok(
        ledgerAccountService.getAllWalletLedgerAccounts().stream()
            .map(ledgerAccountConverter::toDto)
            .collect(Collectors.toSet()));
  }

  @GetMapping("/{accountNumber}")
  public ResponseEntity<?> getAllLedgerAccountsByAccountNumber(@PathVariable String accountNumber) {
    var accounts =
        ledgerAccountService.getAllAccountsByAccountNumber(accountNumber).stream()
            .map(ledgerAccountConverter::toDto)
            .toList();
    return ResponseEntity.ok(accounts);
  }
}
