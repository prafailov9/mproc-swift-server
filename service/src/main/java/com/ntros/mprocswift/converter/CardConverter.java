package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.exceptions.AccountNotFoundException;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.card.CardType;
import com.ntros.mprocswift.repository.account.AccountRepository;
import com.ntros.mprocswift.repository.card.CardTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class CardConverter implements Converter<CardDTO, Card> {

    private final AccountRepository accountRepository;
    private final CardTypeRepository cardTypeRepository;

    @Autowired
    public CardConverter(AccountRepository accountRepository, CardTypeRepository cardTypeRepository) {
        this.accountRepository = accountRepository;
        this.cardTypeRepository = cardTypeRepository;
    }

    @Override
    public CardDTO toDto(Card model) {
        CardDTO dto = new CardDTO();

        dto.setCardIdHash(model.getCardIdHash());
        dto.setCardHolder(getCardHolderName(model));
        dto.setCardProvider(model.getCardProvider());
        dto.setCardNumber(model.getCardNumber());
        dto.setCvv(model.getCvv());
        dto.setExpirationDate(model.getExpirationDate());
        dto.setPin(model.getPin());
        dto.setStatus(model.getStatus().name());

        dto.setType(model.getCardType().getType());
        return dto;
    }

    @Override
    public Card toModel(CardDTO dto) {
        CardType cardType = cardTypeRepository.findByType(dto.getType()).orElseThrow(() -> new IllegalArgumentException(String.format("Card type not found: %s", dto.getType())));

        Card card = new Card();
        card.setCardProvider(dto.getCardProvider());
        card.setCardNumber(dto.getCardNumber());
        card.setCardType(cardType);
        card.setExpirationDate(dto.getExpirationDate());
        card.setCvv(dto.getCvv());
        card.setPin(dto.getPin());

        card.setAccount(accountRepository.findByAccountName(dto.getCardHolder()).orElseThrow(() -> new AccountNotFoundException(format("Account not found for beneficiary: %s", dto.getCardHolder()))));

        return card;
    }

    private String getCardHolderName(Card model) {
        return String.format("%s %s", model.getAccount().getUser().getFirstName(), model.getAccount().getUser().getLastName());
    }
}
