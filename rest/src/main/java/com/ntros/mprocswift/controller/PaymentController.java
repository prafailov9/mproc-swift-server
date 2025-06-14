package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.service.payment.PaymentProcessingService;
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

    private final PaymentProcessingService paymentProcessingService;

    @Autowired
    public PaymentController(final PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }


    @PostMapping("/card-payment")
    public CompletableFuture<ResponseEntity<?>> processPayment(@Validated AuthorizePaymentRequest authorizePaymentRequest) {
        return CompletableFuture
                .supplyAsync(() -> paymentProcessingService.authorizePayment(authorizePaymentRequest))
                .handleAsync(this::handleResponseAsync);
    }

}
