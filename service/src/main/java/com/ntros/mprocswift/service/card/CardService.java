package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.model.card.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CardService {

    Card getCard(final int cardId);

    List<Card> getAllCards();

    CompletableFuture<Card> createCard(final Card card);


}
