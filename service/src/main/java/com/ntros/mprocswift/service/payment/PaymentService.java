package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cartpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cartpayment.CardPaymentResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Service to handle a user's card payments to merchants.
 */
public interface PaymentService {

    CompletableFuture<CardPaymentResponse> processPayment(final CardPaymentRequest cardPaymentRequest);

}
