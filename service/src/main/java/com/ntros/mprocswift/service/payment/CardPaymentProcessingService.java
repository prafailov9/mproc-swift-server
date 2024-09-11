package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.CardPaymentResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CardPaymentProcessingService implements PaymentService {


    @Override
    public CompletableFuture<CardPaymentResponse> processPayment(CardPaymentRequest cardPaymentRequest) {
        return null;
    }
}
