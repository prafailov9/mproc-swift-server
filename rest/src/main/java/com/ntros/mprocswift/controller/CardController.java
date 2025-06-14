package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.converter.CardConverter;
import com.ntros.mprocswift.service.card.CardDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/cards")
public class CardController extends AbstractApiController {

    private final CardDataService cardDataService;
    private final CardConverter cardConverter;
    @Autowired
    public CardController(CardDataService cardDataService, CardConverter cardConverter) {
        this.cardDataService = cardDataService;
        this.cardConverter = cardConverter;
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
        return cardDataService
                .getAllCards()
                .thenApplyAsync(accounts -> accounts
                        .stream()
                        .map(cardConverter::toDto)
                        .collect(Collectors.toList()))
                .handleAsync((this::handleResponseAsync));
    }

}
