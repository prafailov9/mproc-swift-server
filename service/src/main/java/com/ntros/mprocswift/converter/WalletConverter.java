package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.model.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletConverter implements Converter<WalletDTO, Wallet> {

  @Override
  public WalletDTO toDto(Wallet model) {
    WalletDTO form = new WalletDTO();
    form.setCurrencyCode(model.getCurrency().getCurrencyCode());
    form.setAccountNumber(model.getAccount().getAccountDetails().getAccountNumber());
    form.setBalance(BigDecimal.valueOf(model.getBalance()));
    form.setMain(model.isMain());
    return form;
  }

  @Override
  public Wallet toModel(WalletDTO walletDTO) {
    Wallet wallet = new Wallet();
    wallet.setMain(walletDTO.isMain());
    wallet.setBalance(walletDTO.getBalance().longValue());
    return wallet;
  }
}
