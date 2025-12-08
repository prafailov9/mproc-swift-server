package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.CardConverter;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;
import com.ntros.mprocswift.dto.cardpayment.HoldSettlementRequest;
import com.ntros.mprocswift.dto.cardpayment.HoldSettlementResponse;
import com.ntros.mprocswift.service.card.CardDataService;
import com.ntros.mprocswift.service.payment.PaymentProcessingService;
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
    private final PaymentProcessingService paymentProcessingService;

    @Autowired
    public CardController(CardDataService cardDataService, CardConverter cardConverter, PaymentProcessingService paymentProcessingService) {
        this.cardDataService = cardDataService;
        this.cardConverter = cardConverter;
        this.paymentProcessingService = paymentProcessingService;
    }

    /**
     * 1. get all cards
     * 2. get cards by account
     * 3. get card by type + account
     * 5. call pay-with-card api
     */

    @GetMapping
    public ResponseEntity<?> getAllCards() {
        try {
            var cards = cardDataService.getAllCards().stream()
                    .map(cardConverter::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(cards);
        } catch (Exception ex) {
            return handleResponseAsync(null, ex); // or a sync variant
        }
    }

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponse> authorizePayment(@RequestBody AuthorizePaymentRequest authorizePaymentRequest) {
        return ResponseEntity.ok(paymentProcessingService.authorizePayment(authorizePaymentRequest));
    }

    @PostMapping("/settle")
    public ResponseEntity<HoldSettlementResponse> settleHolds(@RequestBody HoldSettlementRequest holdSettlementRequest) {
        return ResponseEntity.ok(paymentProcessingService.settleHold(holdSettlementRequest));
    }

}
