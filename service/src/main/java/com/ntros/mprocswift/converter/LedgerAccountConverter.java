package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.ledger.LedgerAccountDTO;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LedgerAccountConverter implements Converter<LedgerAccountDTO, LedgerAccount> {
  @Override
  public LedgerAccountDTO toDto(LedgerAccount model) {
    LedgerAccountDTO dto = new LedgerAccountDTO();
    dto.setActive(model.isActive());
    dto.setLedgerAccountName(model.getLedgerAccountName());
    dto.setCurrencyCode(model.getCurrency().getCurrencyCode());
    log.info("ledgerAccount: {}, wallet: {}", model, model.getWallet());
    dto.setOwnerAccountNumber(model.getWallet().getAccount().getAccNumber());

    dto.setLedgerAccountType(model.getLedgerAccountType().getTypeCode());
    return dto;
  }

  @Override
  public LedgerAccount toModel(LedgerAccountDTO dto) {
    throw new UnsupportedOperationException("method is not implemented yet");
  }
}
