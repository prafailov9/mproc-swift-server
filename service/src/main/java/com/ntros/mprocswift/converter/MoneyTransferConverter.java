package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.MoneyTransferDTO;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
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
        dto.setStatus(model.getTransaction().getStatus().getStatusName());
        dto.setType(model.getTransaction().getType().getTypeName());
        dto.setTransactionDate(model.getTransaction().getTransactionDate());
        dto.setSourceCurrency(model.getTransaction().getCurrency().getCurrencyCode());
        if (dto.getType().equals("WALLET_TO_WALLET_TRANSFER")) {
            dto.setTargetCurrency(model.getTargetCurrencyCode());
        }
        return dto;
    }

    @Override
    public MoneyTransfer toModel(MoneyTransferDTO dto) {
        return null;
    }
}
