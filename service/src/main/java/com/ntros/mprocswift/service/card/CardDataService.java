package com.ntros.mprocswift.service.card;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.exceptions.CannotRefreshCardException;
import com.ntros.mprocswift.exceptions.CardNotCreatedException;
import com.ntros.mprocswift.exceptions.CardNotFoundException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.card.CardStatus;
import com.ntros.mprocswift.repository.card.CardRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

import static java.lang.String.format;

@Slf4j
@Service
@Transactional
public class CardDataService implements CardService {

    private final Executor executor;
    private final CardRepository cardRepository;

    @Autowired
    public CardDataService(@Qualifier("taskExecutor") Executor executor, CardRepository cardRepository) {
        this.executor = executor;
        this.cardRepository = cardRepository;
    }

    @Override
    public Card getCard(int cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(format("Card with ID: %s not found", cardId)));
    }

    @Override
    public Card getCard(CardDTO cardDTO) {
        return cardRepository
                .findByNumberExpirationCvv(cardDTO.getCardProvider(),
                        cardDTO.getCardNumber(),
                        cardDTO.getExpirationDate(),
                        cardDTO.getCvv())
                .orElseThrow(() -> new NotFoundException("Card not found."));
    }

    @Override
    public Card getCardByHash(String cardIdHash) {
        Card card =
                cardRepository.findByCardIdHash(cardIdHash)
                        .orElseThrow(() -> new NotFoundException(String.format("Card not found for hash: %s", cardIdHash)));
        if (!card.getStatus().equals(CardStatus.ACTIVE)) {
            throw new IllegalArgumentException(String.format("Invalid card status: %s", card.getStatus().name()));
        }
        return card;
    }

    @Override
    public List<Card> getAllCardsForAccountNumber(String accountNumber) {
        return cardRepository.findAllByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new NotFoundException(format("No cards found for account number: %s", accountNumber)));
    }

    @Override
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Override
    public Card createCard(Card card) {
        try {
            return cardRepository.save(card);
        } catch (DataIntegrityViolationException ex) {
            log.error("Could not create card {}. {}", card, ex.getMessage(), ex.getCause());
            throw new CardNotCreatedException(format("Could not create card: %s", ex.getMessage()));
        }
    }

    @Override
    public void deleteCard(Card card) {

    }

    @Override
    public Card refresh(Card card) {
        if (!card.getCardType().getType().equals("ONE_TIME_VIRTUAL")) {
            throw new CannotRefreshCardException("Card must be of type ONE_TIME_VIRTUAL.");
        }

        // TODO: generate new card data(number, exp_date, cvv)
        return null;
    }

}