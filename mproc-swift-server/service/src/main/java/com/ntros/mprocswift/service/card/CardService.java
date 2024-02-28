package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.model.card.Card;

import java.util.List;

public interface CardService {

    Card getCard(final int cardId);

    List<Card> getAllCards();

    Card addCard(final Card card);

}
