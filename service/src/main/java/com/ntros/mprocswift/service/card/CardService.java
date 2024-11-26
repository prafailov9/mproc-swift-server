package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.model.card.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CardService {

    Card getCard(final int cardId);

    CompletableFuture<Card> getCard(String provider, String cardNumber, String expirationDate, String cvv);

    List<Card> getAllCards();

    CompletableFuture<Card> createCard(final Card card);

    void deleteCard(Card card);

    Card refresh(Card card);


}
