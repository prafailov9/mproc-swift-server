package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.AccountConverter;
import com.ntros.mprocswift.dto.RangeRequest;
import com.ntros.mprocswift.dto.account.AccountBatchRequest;
import com.ntros.mprocswift.service.account.AccountBalanceUpdaterService;
import com.ntros.mprocswift.service.account.AccountService;
import com.ntros.mprocswift.service.account.AccountUpdaterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
  private final AccountUpdaterService accountUpdaterService;

  @Autowired
  public AccountController(
      AccountService accountService,
      AccountConverter accountModelConverter,
      AccountUpdaterService accountUpdaterService) {
    this.accountService = accountService;
    this.accountModelConverter = accountModelConverter;
    this.accountUpdaterService = accountUpdaterService;
  }

  @PostMapping("/{accountNumber}/balance-refresh")
  public ResponseEntity<?> refreshBalanceAndFetchAccount(@PathVariable String accountNumber) {
    return ResponseEntity.ok(
        accountModelConverter.toDto(
            accountUpdaterService.updateAndFetchWalletAccount(accountNumber)));
  }

  @GetMapping("/{accountId}")
  public CompletableFuture<ResponseEntity<?>> getAccount(@PathVariable @Min(1) int accountId) {
    return accountService
        .getAccount(accountId)
        .thenApplyAsync(accountModelConverter::toDto)
        .handleAsync(this::handleResponseAsync);
  }

  public CompletableFuture<ResponseEntity<?>> getAccountByANAsync(
      @PathVariable("async/accountNumber") String accountNumber) {
    return accountService
        .getAccountByAccountNumberAsync(accountNumber)
        .thenApplyAsync(accountModelConverter::toDto)
        .handleAsync(this::handleResponseAsync);
  }

  @GetMapping("/an/{accountNumber}")
  public ResponseEntity<?> getAccountByAN(@PathVariable("accountNumber") String accountNumber) {
    var acc = accountService.getAccountByAccountNumber(accountNumber);
    log.info("Loaded account: {}", acc);
    return ResponseEntity.ok(accountModelConverter.toDto(acc));
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

  @PostMapping("/batch")
  public ResponseEntity<?> getAccountsBatch(@RequestBody AccountBatchRequest accountBatchRequest) {
    return ResponseEntity.ok(
        accountService.getBatchAccounts(accountBatchRequest.getAccountNumbers()).stream()
            .map(accountModelConverter::toDto)
            .toList());
  }
}
