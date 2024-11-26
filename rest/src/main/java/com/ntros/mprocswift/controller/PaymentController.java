package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.service.payment.CardPaymentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("api/payment")
public class PaymentController extends AbstractApiController {

    private final CardPaymentProcessingService cardPaymentProcessingService;

    @Autowired
    public PaymentController(final CardPaymentProcessingService cardPaymentProcessingService) {
        this.cardPaymentProcessingService = cardPaymentProcessingService;
    }


    @PostMapping("/card-payment")
    public CompletableFuture<ResponseEntity<?>> processPayment(@Validated CardPaymentRequest cardPaymentRequest) {
        return CompletableFuture
                .supplyAsync(() -> cardPaymentProcessingService.processPayment(cardPaymentRequest))
                .handleAsync(this::handleResponseAsync);
    }

}
