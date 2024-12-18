package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.exceptions.CannotRefreshCardException;
import com.ntros.mprocswift.exceptions.CardNotCreatedException;
import com.ntros.mprocswift.exceptions.CardNotFoundException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.card.CardType;
import com.ntros.mprocswift.repository.CardRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
    public CompletableFuture<Card> getCard(CardDTO cardDTO) {
        return CompletableFuture
                .supplyAsync(() -> cardRepository
                        .findByNumberExpirationCvv(cardDTO.getCardProvider(), cardDTO.getCardNumberHash(), cardDTO.getExpirationDate(), cardDTO.getCvvHash())
                        .orElseThrow(() -> new NotFoundException("Card not found.")));
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
                log.error("Could not create card {}. {}", card, ex.getMessage(), ex.getCause());
                throw new CardNotCreatedException(String.format("Could not create card: %s", ex.getMessage()));
            }
        });
    }

    @Override
    public void deleteCard(Card card) {

    }

    @Override
    public Card refresh(Card card) {
        if (!card.getCardType().equals(CardType.ONE_TIME_VIRTUAL)) {
            throw new CannotRefreshCardException("Card must be of type ONE_TIME_VIRTUAL.");
        }
        
        return null;
    }

}