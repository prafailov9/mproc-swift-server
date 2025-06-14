package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.model.card.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CardService {

    Card getCard(final int cardId);

    CompletableFuture<Card> getCard(CardDTO cardDTO);
    CompletableFuture<List<Card>> getAllCardsForAccountNumber(String accountNumber);

    CompletableFuture<List<Card>> getAllCards();

    CompletableFuture<Card> createCard(final Card card);

    void deleteCard(Card card);

    Card refresh(Card card);


}
