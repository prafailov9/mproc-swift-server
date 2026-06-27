package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.MoneyTransferConverter;
import com.ntros.mprocswift.service.transaction.MoneyTransferService;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/tx")
public class TransactionController extends AbstractApiController {

  private final MoneyTransferService moneyTransferService;
  private final MoneyTransferConverter moneyTransferConverter;

  @Autowired
  public TransactionController(
      final MoneyTransferService moneyTransferService,
      final MoneyTransferConverter moneyTransferConverter) {
    this.moneyTransferService = moneyTransferService;
    this.moneyTransferConverter = moneyTransferConverter;
  }

  @GetMapping("/transfers/{accountNumber}")
  public ResponseEntity<?> getAllTransfersForAccount(
      @PathVariable("accountNumber")
          @Pattern(regexp = "\\d+", message = "ACCOUNT_NUMBER must be a number.")
          String accountNumber) {
    return ResponseEntity.ok(
        moneyTransferService.getAllTransfersForAccount(accountNumber).stream()
            .map(moneyTransferConverter::toDto)
            .toList());
  }
}
