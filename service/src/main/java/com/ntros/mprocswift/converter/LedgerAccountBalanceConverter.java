package com.ntros.mprocswift.converter;

import com.ntros.mprocswift.dto.ledger.LedgerAccountBalanceDto;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
import com.ntros.mprocswift.model.ledger.LedgerAccountBalance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LedgerAccountBalanceConverter
    implements Converter<LedgerAccountBalanceDto, LedgerAccountBalance> {
  @Override
  public LedgerAccountBalanceDto toDto(LedgerAccountBalance model) {
    var dto = new LedgerAccountBalanceDto();
    dto.setBalance(model.getBalanceMinor());
    dto.setUpdatedAt(model.getUpdatedAt().toString());
    var acc = model.getLedgerAccount();
    dto.setType(acc.getLedgerAccountType().getTypeCode());
    dto.setOwnerId(resolveOwner(acc));
    return dto;
  }

  private String resolveOwner(LedgerAccount ledgerAccount) {
    if (isInternalAccountOwned(ledgerAccount)) {
      return "W:" + ledgerAccount.getWallet().getAccount().getAccNumber();
    } else if (isExternalAccountOwned(ledgerAccount)) {
      return "EXT:" + ledgerAccount.getExternalAccount().getAccountDetails().getAccountNumber();
    } else if (isMerchantAccountOwned(ledgerAccount)) {
      return "M:" + ledgerAccount.getMerchant().getMerchantIdentifierCode();
    } else {
      return "SYSTEM_ACCOUNT";
    }
  }

  private boolean isInternalAccountOwned(LedgerAccount ledgerAccount) {
    return ledgerAccount.getWallet() != null
        && ledgerAccount.getExternalAccount() == null
        && ledgerAccount.getMerchant() == null;
  }

  private boolean isExternalAccountOwned(LedgerAccount ledgerAccount) {
    return ledgerAccount.getWallet() == null
        && ledgerAccount.getExternalAccount() != null
        && ledgerAccount.getMerchant() == null;
  }

  private boolean isMerchantAccountOwned(LedgerAccount ledgerAccount) {
    return ledgerAccount.getWallet() == null
        && ledgerAccount.getExternalAccount() == null
        && ledgerAccount.getMerchant() != null;
  }

  @Override
  public LedgerAccountBalance toModel(LedgerAccountBalanceDto dto) {
    throw new UnsupportedOperationException("Cannot convert Ledger Account Balances to model");
  }
}
