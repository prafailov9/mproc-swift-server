package com.ntros.mprocswift.service.payment;

import static com.ntros.mprocswift.utils.TextFormatter.format;

import com.ntros.mprocswift.dto.cardpayment.*;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import com.ntros.mprocswift.model.transactions.card.HoldSettlement;
import com.ntros.mprocswift.service.account.AccountService;
import com.ntros.mprocswift.service.card.CardService;
import com.ntros.mprocswift.service.currency.CurrencyDataService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateDataService;
import com.ntros.mprocswift.service.merchant.MerchantService;
import com.ntros.mprocswift.service.transaction.AuthPaymentContext;
import com.ntros.mprocswift.service.transaction.TransactionService;
import com.ntros.mprocswift.service.wallet.WalletService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PaymentProcessingService implements PaymentService {

  private final MerchantService merchantService;
  private final CardService cardService;
  private final CurrencyDataService currencyDataService;
  private final CurrencyExchangeRateDataService currencyExchangeRateDataService;
  private final TransactionService transactionService;
  private final WalletService walletService;
  private final AccountService accountService;

  @Autowired
  public PaymentProcessingService(
      MerchantService merchantService,
      CardService cardService,
      CurrencyDataService currencyDataService,
      CurrencyExchangeRateDataService currencyExchangeRateDataService,
      TransactionService transactionService,
      WalletService walletService,
      AccountService accountService) {

    this.merchantService = merchantService;
    this.cardService = cardService;
    this.currencyDataService = currencyDataService;
    this.currencyExchangeRateDataService = currencyExchangeRateDataService;
    this.transactionService = transactionService;
    this.walletService = walletService;
    this.accountService = accountService;
  }

  @Override
  @Transactional
  public AuthorizePaymentResponse authorizePayment(AuthorizePaymentRequest paymentRequest) {
    AuthorizePaymentResponse response;
    try {
      // get db data
      Card card = cardService.getCardByHash(paymentRequest.getCardIdentifier());
      Merchant merchant = merchantService.getOrCreateMerchant(paymentRequest);
      Currency currency = currencyDataService.getCurrencyByCode(paymentRequest.getCurrency());

      // wallet is locked so only one tx at a time can use it
      Wallet lockedWallet =
          walletService.getLockedWallet(getAvailableWallet(card, paymentRequest).getWalletId());

      // run FX conversion if needed
      BigDecimal convertedAmount = getAmount(lockedWallet, paymentRequest);

      // Sum active holds for this wallet inside the same TX
      BigDecimal existingHolds = transactionService.getHoldAmountSumForWallet(lockedWallet);
      BigDecimal totalReserved = existingHolds.add(convertedAmount);

      if (!lockedWallet.hasAvailableBalance(totalReserved)) {
        throw new InsufficientFundsException(
            format(
                "Insufficient funds. Requested (incl. holds): %s, Balance: %s",
                totalReserved, lockedWallet.getBalance()));
      }
      AuthPaymentContext ctx =
          new AuthPaymentContext(card, merchant, lockedWallet, convertedAmount, currency);

      // reserve money
      String authCode = transactionService.placeHold(ctx);
      response = buildSuccessAuthorizationResponse(ctx, authCode);

    } catch (Exception ex) {
      log.error(
          "Authorization hold request failed. cardIdHash={}, merchant={}, amount={}, currency={}",
          paymentRequest.getCardIdentifier(),
          paymentRequest.getMerchant(),
          paymentRequest.getAmount(),
          paymentRequest.getCurrency(),
          ex);
      response = buildFailedAuthorizationResponse(paymentRequest, ex.getMessage());
    }
    return response;
  }

  /** Settling the reserved amount. Payment must be authorized. */
  @Transactional
  @Override
  public HoldSettlementResponse settlePayment(HoldSettlementRequest holdSettlementRequest) {
    // idempotency check
    HoldSettlement existingSettlement =
        transactionService.getHoldSettlement(holdSettlementRequest.getAuthCode());
    AuthorizedHold hold = transactionService.getAuthorizedHold(holdSettlementRequest.getAuthCode());
    if (existingSettlement != null) {
      log.info(
          "Transaction already settled for this AuthCode: {}; HoldSettlement: {}",
          holdSettlementRequest.getAuthCode(),
          existingSettlement);
      return getHoldSettlementResponse(hold);
    }
    // get tx records
    CardAuthorization cardAuth = hold.getCardAuthorization();
    Transaction baseTx = cardAuth.getTransaction();
    if (!baseTx.getStatus().getStatusName().equals(AuthCodeType.AUTHORIZED.name())) {
      throw new IllegalArgumentException(
          String.format("Settlement not authorized. Tx: %s", baseTx));
    }

    // deduct the amount from account
    BigDecimal amountToSettle = hold.getHoldAmount();
    Wallet wallet = hold.getWallet();
    Account account = wallet.getAccount();

    if (!wallet.hasAvailableBalance(amountToSettle)) {
      throw new InsufficientFundsException(
          String.format(
              "Wallet {%s} has insufficient funds. Amount to settle: %s, wallet balance: %s",
              wallet, amountToSettle, wallet.getBalance()));
    }
    // update balances and card
    wallet.decreaseBalance(amountToSettle);
    walletService.updateBalance(wallet.getWalletId(), wallet.getBalance());
    Account accountNewBalance = accountService.updateTotalBalance(account);
    cardAuth.getCard().setAccount(accountNewBalance);

    // create settlement tx
    transactionService.settleHold(
        cardAuth,
        hold,
        new AuthPaymentContext(
            cardAuth.getCard(),
            cardAuth.getMerchant(),
            wallet,
            amountToSettle,
            wallet.getCurrency()));

    // build response
    return getHoldSettlementResponse(hold);
  }

  @NotNull
  private static HoldSettlementResponse getHoldSettlementResponse(AuthorizedHold hold) {
    CardAuthorization cardAuth = hold.getCardAuthorization();

    HoldSettlementResponse response = new HoldSettlementResponse();
    response.setSuccess(true);
    response.setDescription("Successfully settled holds");
    response.setSettledAmount(hold.getHoldAmount().toString());
    response.setCurrencyCode(hold.getWallet().getCurrency().getCurrencyCode());
    response.setMerchant(cardAuth.getMerchant().getMerchantName());
    return response;
  }

  /**
   * try to get wallet from request(req.currency) if not exist, default to main wallet, or first
   * available wallet
   */
  private Wallet getAvailableWallet(Card card, AuthorizePaymentRequest request) {
    Account account = card.getAccount();
    if (account == null) {
      throw new NotFoundException("Account does not exist.");
    }
    if (account.getWallets().isEmpty()) {
      throw new IllegalArgumentException(
          String.format("No available wallets for Account: %s", account));
    }
    return account
        .getWalletByCurrencyCode(request.getCurrency())
        .orElse(account.getMainWallet().orElse(account.getWallets().get(0)));
  }

  private BigDecimal getAmount(Wallet wallet, AuthorizePaymentRequest authorizePaymentRequest) {
    return wallet.getCurrency().getCurrencyCode().equals(authorizePaymentRequest.getCurrency())
        ? BigDecimal.valueOf(authorizePaymentRequest.getAmount())
        : currencyExchangeRateDataService.convert(
            BigDecimal.valueOf(authorizePaymentRequest.getAmount()),
            wallet.getCurrency().getCurrencyCode(),
            authorizePaymentRequest.getCurrency());
  }

  private AuthorizePaymentResponse buildSuccessAuthorizationResponse(
      AuthPaymentContext ctx, String authCode) {
    AuthorizePaymentResponse response = new AuthorizePaymentResponse();
    response.setStatus(RequestResultStatus.SUCCESS);
    response.setMessage(format("Payment to %s Authorized", ctx.merchant().getMerchantName()));
    response.setMerchant(ctx.merchant().getMerchantName());
    response.setCurrency(ctx.wallet().getCurrency().getCurrencyCode());
    response.setAccountNumber(ctx.wallet().getAccount().getAccountDetails().getAccountNumber());
    response.setPrice(ctx.authorizedAmount().doubleValue());

    response.setAuthCode(authCode);

    return response;
  }

  private AuthorizePaymentResponse buildFailedAuthorizationResponse(
      AuthorizePaymentRequest request, String error) {
    AuthorizePaymentResponse response = new AuthorizePaymentResponse();
    response.setStatus(RequestResultStatus.FAILED);
    response.setMessage(
        format("Failed to authorize payment to %s. Error: [%s]", request.getMerchant(), error));
    response.setMerchant(request.getMerchant());
    response.setCurrency(request.getCurrency());
    response.setPrice(request.getAmount());

    response.setAuthCode(AuthCodeType.UNAUTHORIZED.name());

    return response;
  }
}
