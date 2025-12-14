package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.CurrencyConverter;
import com.ntros.mprocswift.service.currency.CurrencyService;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/currency")
public class CurrencyController extends AbstractApiController {

  private final CurrencyService currencyService;
  private final CurrencyConverter currencyConverter;

  @Autowired
  public CurrencyController(
      final CurrencyService currencyService, final CurrencyConverter currencyConverter) {
    this.currencyService = currencyService;
    this.currencyConverter = currencyConverter;
  }

  @GetMapping("/{currencyCode}")
  public CompletableFuture<ResponseEntity<?>> getCurrency(@PathVariable String currencyCode) {
    return currencyService
        .getCurrencyByCodeAsync(currencyCode)
        .thenApplyAsync(currencyConverter::toDto)
        .handleAsync(this::handleResponseAsync);
  }

  @PatchMapping("/activate-all")
  public CompletableFuture<ResponseEntity<String>> activateAllCurrencies() {
    return CompletableFuture.supplyAsync(currencyService::activateAll)
        .thenApplyAsync(voidCompletableFuture -> ResponseEntity.ok("All currencies activated"));
  }

  @DeleteMapping("/{currencyId}")
  public CompletableFuture<ResponseEntity<?>> deleteCurrency(
      @PathVariable("currencyId") @Min(1) @Validated final int currencyId) {
    return currencyService.deleteCurrency(currencyId).handleAsync(this::handleResponseAsync);
  }
}
