package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.dto.cartpayment.CardPaymentRequest;
import com.ntros.mprocswift.service.payment.CardPaymentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/cards")
public class CardController extends AbstractApiController {

    private final CardPaymentProcessingService cardPaymentProcessingService;

    @Autowired
    public CardController(final CardPaymentProcessingService cardPaymentProcessingService) {
        this.cardPaymentProcessingService = cardPaymentProcessingService;
    }

    /**
     * 1. get all cards
     * 2. get cards by account
     * 3. get card by type + account
     * 4. create card
     * 5. call pay-with-card api
     */

    @PostMapping("/card-payment")
    public CompletableFuture<ResponseEntity<?>> processCardPayment(@Validated CardPaymentRequest cardPaymentRequest) {
        return CompletableFuture
                .supplyAsync(() -> cardPaymentProcessingService.processPayment(cardPaymentRequest))
                .handleAsync(this::handleResponseAsync);
    }
}
