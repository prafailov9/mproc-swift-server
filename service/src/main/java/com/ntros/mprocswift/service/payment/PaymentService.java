package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Service to handle a user's card payments to merchants.
 */
public interface PaymentService {

    AuthorizePaymentResponse authorizePayment(final AuthorizePaymentRequest authorizePaymentRequest);

}
