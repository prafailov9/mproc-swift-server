package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.AccountConverter;
import com.ntros.mprocswift.dto.RangeRequest;
import com.ntros.mprocswift.service.account.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/accounts")
@Slf4j
public class AccountController extends AbstractApiController {

  private final AccountService accountService;
  private final AccountConverter accountModelConverter;

  @Autowired
  public AccountController(
      final AccountService accountService, final AccountConverter accountModelConverter) {
    this.accountService = accountService;
    this.accountModelConverter = accountModelConverter;
  }

  @GetMapping("/{accountId}")
  public CompletableFuture<ResponseEntity<?>> getAccount(@PathVariable @Min(1) int accountId) {
    return accountService
        .getAccount(accountId)
        .thenApplyAsync(accountModelConverter::toDto)
        .handleAsync(this::handleResponseAsync);
  }

  @GetMapping("/an/{accountNumber}")
  public CompletableFuture<ResponseEntity<?>> getAccountByAN(
      @PathVariable("accountNumber")
          @Pattern(regexp = "\\d+", message = "ACCOUNT_NUMBER must be a number.")
          String accountNumber) {
    return accountService
        .getAccountByAccountNumber(accountNumber)
        .thenApplyAsync(accountModelConverter::toDto)
        .handleAsync(this::handleResponseAsync);
  }

  @GetMapping("/by-currency/{currencyCode}")
  public ResponseEntity<?> getAccountsByCurrency(
      @PathVariable("currencyCode")
          @Pattern(regexp = "^[A-Za-z]{3}$", message = "CURRENCY_CODE must be a string.")
          String currencyCode) {
    return ResponseEntity.ok(
        accountService.getAllAccountsByCurrencyCode(currencyCode).stream()
            .map(accountModelConverter::toDto)
            .collect(Collectors.toList()));
  }

  @GetMapping
  public CompletableFuture<ResponseEntity<?>> getAllAccounts() {
    return accountService
        .getAllAccounts()
        .thenApplyAsync(
            accounts ->
                accounts.stream().map(accountModelConverter::toDto).collect(Collectors.toList()))
        .handleAsync((this::handleResponseAsync));
  }

  @GetMapping("by-wallets/{walletCount}")
  public CompletableFuture<ResponseEntity<?>> getAllAccountsByWalletCount(
      @PathVariable @Min(1) final int walletCount) {
    return accountService
        .getAllAccountsWalletCount(walletCount)
        .thenApplyAsync(
            accounts ->
                accounts.stream().map(accountModelConverter::toDto).collect(Collectors.toList()))
        .handleAsync((this::handleResponseAsync));
  }

  @GetMapping("by-wallets/range")
  public CompletableFuture<ResponseEntity<?>> getAllAccountsByWalletCountRange(
      @RequestBody @Valid RangeRequest rangeRequest) {
    return accountService
        .getAllAccountsByWalletCountInRange(rangeRequest.getOrigin(), rangeRequest.getBound())
        .thenApplyAsync(
            accountsByRange ->
                accountsByRange.stream()
                    .map(
                        accounts ->
                            accounts.stream().map(accountModelConverter::toAccountWalletCountDTO))
                    .collect(Collectors.toList()))
        .handleAsync((this::handleResponseAsync));
  }

  @PatchMapping("update/all")
  public ResponseEntity<?> updateTotalBalanceForAllAccounts() {
    String res = accountService.calculateTotalBalanceForAllAccounts();
    return ResponseEntity.ok().body(res);
  }
}
