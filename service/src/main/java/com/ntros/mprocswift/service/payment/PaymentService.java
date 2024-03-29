package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.CardPaymentResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Service to handle a user's card payments to merchants.
 */
public interface PaymentService {

    CompletableFuture<CardPaymentResponse> processPayment(final CardPaymentRequest cardPaymentRequest);

}
