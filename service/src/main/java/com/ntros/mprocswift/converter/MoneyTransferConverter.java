package com.ntros.mprocswift.converter;

import static com.ntros.mprocswift.model.currency.MoneyConverter.toMajor;

import com.ntros.mprocswift.dto.MoneyTransferDTO;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.transactions.MoneyTransfer;
import org.springframework.stereotype.Component;

@Component
public class MoneyTransferConverter implements Converter<MoneyTransferDTO, MoneyTransfer> {

  @Override
  public MoneyTransferDTO toDto(MoneyTransfer model) {
    Currency currency = model.getTransaction().getCurrency();
    MoneyTransferDTO dto = new MoneyTransferDTO();
    dto.setAmount(toMajor(model.getTransaction().getAmount(), currency.getMinorUnits()));
    dto.setFees(toMajor(model.getTransaction().getFees(), currency.getMinorUnits()));
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
