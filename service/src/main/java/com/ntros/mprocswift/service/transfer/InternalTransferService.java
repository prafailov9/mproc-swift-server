package com.ntros.mprocswift.service.transfer;

import static com.ntros.mprocswift.model.transactions.TxnType.INTERNAL;
import static com.ntros.mprocswift.service.currency.CurrencyUtils.BASE_CURRENCIES;

import com.ntros.mprocswift.converter.FxLegConverter;
import com.ntros.mprocswift.converter.FxQuoteConverter;
import com.ntros.mprocswift.dto.transfer.InternalTransferRequest;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.TxnType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
@Slf4j
public class InternalTransferService extends AbstractTransferService<InternalTransferRequest> {
  public InternalTransferService(
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
  protected LockedWalletGroup getWalletGroup(InternalTransferRequest request) {
    // 1. get wallets for both accounts locked
    var senderWallets = walletService.getAllWalletsLocked(request.getSourceAccountNumber());
    var receiverWallets = walletService.getAllWalletsLocked(request.getRecipientAccountNumber());
    var requestedCurrency = request.getCurrencyCode();
    // find one by currency; throws if non found
    var sender = getWalletByCurrency(senderWallets, requestedCurrency);
    // for receiver, check direct currency,else if main/base-currency wallets exist, else throw
    var receiver = getReceiverWallet(receiverWallets, requestedCurrency);
    return new LockedWalletGroup(sender, receiver);
  }

  @Override
  protected String requestToStr(InternalTransferRequest request) {
    return request.getRequestId()
        + "_"
        + request.getSourceAccountNumber()
        + "_"
        + new BigDecimal(request.getAmount().toString()).stripTrailingZeros().toPlainString()
        + "_"
        + request.getCurrencyCode()
        + "_"
        + request.getSourceAccountNumber();
  }

  @Override
  protected TxnType getTxnType() {
    return INTERNAL;
  }

  /**
   *
   *
   * <pre>
   * Find correct wallet for receiver in the following order:
   * 1. try find wallet with requestedCurrency
   * 2. else, try find main wallet
   * 3. else, try find base
   * Throw if none found
   * </pre>
   */
  private Wallet getReceiverWallet(List<Wallet> wallets, String requestedCurrency) {
    Map<String, Wallet> walletsByCurrency = new HashMap<>();
    for (var wallet : wallets) {
      walletsByCurrency.put(wallet.getCurrency().getCurrencyCode(), wallet);
    }

    return Optional.of(walletsByCurrency.get(requestedCurrency))
        .orElse(
            walletsByCurrency.values().stream()
                .filter(Wallet::isMain)
                .findFirst()
                .orElse(
                    walletsByCurrency.entrySet().stream()
                        .filter(
                            entry -> {
                              for (var base : BASE_CURRENCIES) {
                                if (entry.getKey().equalsIgnoreCase(base)) {
                                  return true;
                                }
                              }
                              return false;
                            })
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(
                            () ->
                                new NotFoundException(
                                    String.format(
                                        "Account: %s does have main wallet, wallet in requested currency: %s or wallet in base currencies: %s",
                                        wallets.getFirst().getAccount().getAccNumber(),
                                        requestedCurrency,
                                        BASE_CURRENCIES)))));
  }
}
