package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.exceptions.CardNotFoundException;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.repository.CardRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class CardDataService implements CardService {

    protected static final Logger log = LoggerFactory.getLogger(CardDataService.class);
    private final CardRepository cardRepository;

    @Autowired
    public CardDataService(final CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    public Card getCard(int cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(String.format("Card with ID: %s not found", cardId)));
    }

    @Override
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Override
    public CompletableFuture<Card> createCard(Card card) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return cardRepository.save(card);
            } catch (DataIntegrityViolationException ex) {
                log.error("Could not create card {}. {}", card, ex.getMessage());
                throw new RuntimeException("card not created");
            }
        });
    }

}