package com.ntros.mprocswift.service.transfer;

import static com.ntros.mprocswift.model.transactions.TxnType.W2W;

import com.ntros.mprocswift.converter.FxLegConverter;
import com.ntros.mprocswift.converter.FxQuoteConverter;
import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import com.ntros.mprocswift.exceptions.WalletNotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.currency.*;
import com.ntros.mprocswift.model.transactions.*;
import com.ntros.mprocswift.repository.currency.CurrencyRepository;
import com.ntros.mprocswift.repository.transaction.MoneyTransferRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.service.currency.audit.FxLegService;
import com.ntros.mprocswift.service.currency.audit.FxQuoteService;
import com.ntros.mprocswift.service.currency.exchangerate.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.currency.exchangerate.FxRateConversionService;
import com.ntros.mprocswift.service.idempotency.IdempotencyKeyMarkingService;
import com.ntros.mprocswift.service.ledger.LedgerAccountBalanceService;
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.wallet.WalletService;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
@Slf4j
public class W2WTransferService extends AbstractTransferService<W2WTransferRequest> {

  // 1 major

  public W2WTransferService(
      PlatformTransactionManager platformTransactionManager,
      TransactionRepository transactionRepository,
      TransactionTypeRepository transactionTypeRepository,
      TransactionStatusRepository transactionStatusRepository,
      MoneyTransferRepository moneyTransferRepository,
      CurrencyRepository currencyRepository,
      WalletService walletService,
      CurrencyExchangeRateService currencyExchangeRateService,
      LedgerAccountService ledgerAccountService,
      LedgerEntryService ledgerEntryService,
      LedgerAccountBalanceService ledgerAccountBalanceService,
      IdempotencyKeyMarkingService idempotencyKeyService,
      FxQuoteService fxQuoteService,
      FxLegService fxLegService,
      FxRateConversionService fxRateConversionService,
      FxQuoteConverter fxQuoteConverter,
      FxLegConverter fxLegConverter) {
    super(
        platformTransactionManager,
        transactionRepository,
        transactionTypeRepository,
        transactionStatusRepository,
        moneyTransferRepository,
        currencyRepository,
        walletService,
        currencyExchangeRateService,
        ledgerAccountService,
        ledgerEntryService,
        ledgerAccountBalanceService,
        idempotencyKeyService,
        fxQuoteService,
        fxLegService,
        fxRateConversionService,
        fxQuoteConverter,
        fxLegConverter);
  }

  @Override
  protected LockedWalletGroup getWalletGroup(W2WTransferRequest request) {
    // validate request
    if (request.getCurrencyCode().equals(request.getToCurrencyCode())) {
      throw new IllegalArgumentException(
          String.format(
              "W2W error: source and target currencies are equal: %s", request.getCurrencyCode()));
    }

    // 1. lock all wallets for this account number, prevents deadlocks + ensures both rows locked
    List<Wallet> lockedWallets =
        walletService.getAllWalletsLocked(request.getSourceAccountNumber());

    // 2. get sender/receiver by currency
    Wallet sender =
        lockedWallets.stream()
            .filter(w -> w.getCurrency().getCurrencyCode().equals(request.getCurrencyCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new WalletNotFoundException(
                        "Sender wallet not found for currency " + request.getCurrencyCode()));

    Wallet receiver =
        lockedWallets.stream()
            .filter(w -> w.getCurrency().getCurrencyCode().equals(request.getToCurrencyCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new WalletNotFoundException(
                        "Receiver wallet not found for currency " + request.getToCurrencyCode()));

    return new LockedWalletGroup(sender, receiver);
  }

  @Override
  protected TxnType getTxnType() {
    return W2W;
  }

  @Override
  protected String requestToStr(W2WTransferRequest request) {
    return request.getRequestId()
        + "_"
        + request.getSourceAccountNumber()
        + "_"
        + new BigDecimal(request.getAmount().toString()).stripTrailingZeros().toPlainString()
        + "_"
        + request.getCurrencyCode()
        + "_"
        + request.getToCurrencyCode();
  }
}
