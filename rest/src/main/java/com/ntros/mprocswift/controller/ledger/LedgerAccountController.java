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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/ledgerAccount")
public class LedgerAccountController extends AbstractApiController {

    private final LedgerAccountService ledgerAccountService;
    private final LedgerAccountConverter ledgerAccountConverter;

    @Autowired
    public LedgerAccountController(LedgerAccountService ledgerAccountService, LedgerAccountConverter ledgerAccountConverter) {
        this.ledgerAccountService = ledgerAccountService;
        this.ledgerAccountConverter = ledgerAccountConverter;
    }

    //    @GetMapping("/all")
    public CompletableFuture<ResponseEntity<?>> getAllLedgerAccounts() {
        return CompletableFuture.supplyAsync(ledgerAccountService::getAllWalletLedgerAccounts)
                .thenApplyAsync(ledgerAccounts -> ledgerAccounts.stream().map(ledgerAccountConverter::toDto).collect(Collectors.toList())).handleAsync(this::handleResponseAsync);
    }

    @GetMapping("/all")
    public ResponseEntity<Set<LedgerAccountDTO>> getAllLedgerAccountsV2() {
        return ResponseEntity.ok(ledgerAccountService.getAllWalletLedgerAccounts().stream().map(ledgerAccountConverter::toDto).collect(Collectors.toSet()));
    }


}
