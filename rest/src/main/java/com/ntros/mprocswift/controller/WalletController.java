package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.WalletConverter;
import com.ntros.mprocswift.dto.UniqueWalletDTO;
import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.service.wallet.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/wallets")
public class WalletController extends AbstractApiController {

    private final WalletService walletService;
    private final WalletConverter walletModelConverter;

    @Autowired
    public WalletController(
            final WalletService walletService,
            final WalletConverter walletModelConverter) {
        this.walletService = walletService;
        this.walletModelConverter = walletModelConverter;
    }

    @GetMapping("/{walletId}")
    @ResponseBody
    public CompletableFuture<ResponseEntity<?>> getWallet(@PathVariable int walletId) {
        return walletService.getWallet(walletId)
                .thenApplyAsync(walletModelConverter::toDTO, executor)
                .handleAsync((walletForm, ex) -> handleResponseAsync(walletForm, ex, HttpStatus.NOT_FOUND), executor);
    }

    @GetMapping("/all")
    @ResponseBody
    public CompletableFuture<ResponseEntity<?>> getAllWallets() {
        return walletService.getAllWallets()
                .thenApplyAsync(wallets -> wallets
                        .stream()
                        .map(walletModelConverter::toDTO)
                        .collect(Collectors.toList()), executor)
                .handleAsync(((walletForms, ex) -> handleResponseAsync(walletForms, ex, HttpStatus.NOT_FOUND)), executor);
    }

    @GetMapping("/account/{accountId}")
    @ResponseBody
    public CompletableFuture<ResponseEntity<?>> getAllWalletsForAccount(@PathVariable final int accountId) {
        return walletService.getAllWalletsByAccount(accountId)
                .thenApplyAsync(wallets -> wallets
                        .stream()
                        .map(walletModelConverter::toDTO)
                        .collect(Collectors.toList()), executor)
                .handleAsync((walletForms, ex) ->
                        handleResponseAsync(walletForms, ex, HttpStatus.NOT_FOUND), executor);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> createWallet(@RequestBody @Validated WalletDTO walletDTO) {
        return walletService.createWallet(walletDTO)
                .handleAsync((wallet, ex) -> handleResponseAsync(walletDTO, ex, HttpStatus.CONFLICT), executor);
    }

    @DeleteMapping
    public CompletableFuture<ResponseEntity<?>> deleteWallet(@RequestBody @Validated UniqueWalletDTO uniqueWalletDTO) {
        return walletService.deleteWallet(uniqueWalletDTO)
                .handleAsync(
                        (affectedRows, ex) ->
                                handleDeleteResponse(affectedRows, ex,
                                        uniqueWalletDTO.getCurrencyCode(),
                                        uniqueWalletDTO.getAccountNumber()), executor);
    }

    private ResponseEntity<?> handleDeleteResponse(int affectedRows, Throwable ex, String code, String an) {
        String responseBody = null;
        if (affectedRows == 1) {
            responseBody = String.format("Successfully deleted wallet: [%s, %s]", code, an);
        } else if (affectedRows <= 0) {
            responseBody = String.format("No wallets modified: [%s, %s]", code, an);
        }
        return handleResponseAsync(responseBody, ex);
    }

}
