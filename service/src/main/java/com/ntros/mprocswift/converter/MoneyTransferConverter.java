package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.MoneyTransferDTO;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import com.ntros.mprocswift.model.transactions.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class MoneyTransferConverter implements Converter<MoneyTransferDTO, MoneyTransfer> {
    @Override
    public MoneyTransferDTO toDto(MoneyTransfer model) {
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(model.getTransaction().getAmount());
        dto.setFees(model.getTransaction().getFees());
        dto.setDescription(model.getTransaction().getDescription());
        dto.setSenderAccountNumber(model.getSenderAccount().getAccountDetails().getAccountNumber());
        dto.setReceiverAccountNumber(model.getReceiverAccount().getAccountDetails().getAccountNumber());
        dto.setStatus(model.getTransaction().getStatus().name());
        dto.setType(model.getTransaction().getType().name());
        dto.setTransactionDate(model.getTransaction().getTransactionDate());
        dto.setSourceCurrency(model.getTransaction().getCurrency().getCurrencyCode());
        if (dto.getType().equals(TransactionType.WALLET_TO_WALLET_TRANSFER.name())) {
            dto.setTargetCurrency(model.getTargetCurrencyCode());
        }
        return dto;
    }

    @Override
    public MoneyTransfer toModel(MoneyTransferDTO dto) {
        return null;
    }
}
