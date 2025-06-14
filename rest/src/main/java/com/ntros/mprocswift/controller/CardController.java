package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.CardConverter;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;
import com.ntros.mprocswift.service.card.CardDataService;
import com.ntros.mprocswift.service.payment.CardPaymentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/cards")
public class CardController extends AbstractApiController {

    private final CardDataService cardDataService;
    private final CardConverter cardConverter;
    private final CardPaymentProcessingService cardPaymentProcessingService;

    @Autowired
    public CardController(CardDataService cardDataService, CardConverter cardConverter, CardPaymentProcessingService cardPaymentProcessingService) {
        this.cardDataService = cardDataService;
        this.cardConverter = cardConverter;
        this.cardPaymentProcessingService = cardPaymentProcessingService;
    }

    /**
     * 1. get all cards
     * 2. get cards by account
     * 3. get card by type + account
     * 4. create card
     * 5. call pay-with-card api
     */


    @GetMapping
    public CompletableFuture<ResponseEntity<?>> getAllCards() {
        return CompletableFuture.supplyAsync(cardDataService::getAllCards)
                .thenApplyAsync(accounts -> accounts
                        .stream()
                        .map(cardConverter::toDto)
                        .collect(Collectors.toList()))
                .handleAsync((this::handleResponseAsync));
    }


    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponse> authorizePayment(@RequestBody AuthorizePaymentRequest authorizePaymentRequest) {
        return ResponseEntity.ok(cardPaymentProcessingService.authorizePayment(authorizePaymentRequest));
    }


}
