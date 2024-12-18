package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.exceptions.AccountNotFoundException;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.card.CardType;
import com.ntros.mprocswift.repository.account.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class CardConverter implements Converter<CardDTO, Card> {

    private final AccountRepository accountRepository;

    @Autowired
    public CardConverter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public CardDTO toDTO(Card model) {
        CardDTO dto = new CardDTO();
        dto.setCardHolder(model.getAccount().getAccountDetails().getAccountName());
        dto.setCardProvider(model.getCardProvider());
        dto.setCardNumberHash(model.getCardNumber());
        dto.setCvvHash(model.getCvv());
        dto.setExpirationDate(model.getExpirationDate());
        dto.setType(model.getCardType().name());
        return dto;
    }

    @Override
    public Card toModel(CardDTO dto) {
        Card card = new Card();
        card.setCardProvider(dto.getCardProvider());
        card.setCardNumber(dto.getCardNumberHash());
        card.setCardType(CardType.valueOf(dto.getType()));
        card.setExpirationDate(dto.getExpirationDate());
        card.setCvv(dto.getCvvHash());
        card.setPinHash(dto.getPinHash());

        card.setAccount(accountRepository.findByAccountName(dto.getCardHolder())
                .orElseThrow(() -> new AccountNotFoundException(format("Account not found for beneficiary: %s", dto.getCardHolder()))));

        return card;
    }
}
