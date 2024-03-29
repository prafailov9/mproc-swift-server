package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cartpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cartpayment.CardPaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
public class CardPaymentProcessingService implements PaymentService {

    private final WebClient webClient;

    public CardPaymentProcessingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://localhost:8081").build();
    }

    @Override
    public CompletableFuture<CardPaymentResponse> processPayment(CardPaymentRequest cardPaymentRequest) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path("https://localhost:8081/api/payment").build())
                .bodyValue(cardPaymentRequest)
                .retrieve()
                .bodyToMono(CardPaymentResponse.class)
                .toFuture();


    }
}
