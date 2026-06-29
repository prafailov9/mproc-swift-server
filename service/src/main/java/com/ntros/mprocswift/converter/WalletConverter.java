package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.WalletDTO;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.MoneyConverter;
import org.springframework.stereotype.Component;

@Component
public class WalletConverter implements Converter<WalletDTO, Wallet> {

  @Override
  public WalletDTO toDto(Wallet model) {
    WalletDTO form = new WalletDTO();
    form.setCurrencyCode(model.getCurrency().getCurrencyCode());
    form.setAccountNumber(model.getAccount().getAccountDetails().getAccountNumber());
    form.setBalance(MoneyConverter.toMajor(model.getBalance(), model.getCurrency().getExponent()));
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
