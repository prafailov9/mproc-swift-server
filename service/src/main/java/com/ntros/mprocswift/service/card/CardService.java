package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.model.card.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CardService {

    Card getCard(final int cardId);

    Card getCard(CardDTO cardDTO);
    Card getCardByHash(String cardIdHash);
    List<Card> getAllCardsForAccountNumber(String accountNumber);

    List<Card> getAllCards();

    Card createCard(final Card card);

    void deleteCard(Card card);

    Card refresh(Card card);


}
