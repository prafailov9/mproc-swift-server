package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.AuthCodeType;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;
import com.ntros.mprocswift.dto.cardpayment.RequestResultStatus;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.card.CardStatus;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.service.card.CardService;
import com.ntros.mprocswift.service.currency.CurrencyDataService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateDataService;
import com.ntros.mprocswift.service.merchant.MerchantService;
import com.ntros.mprocswift.service.transaction.AuthPaymentContext;
import com.ntros.mprocswift.service.transaction.TransactionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

@Service
@Transactional
@Slf4j
public class CardPaymentProcessingService implements PaymentService {

    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final CurrencyDataService currencyDataService;
    private final CurrencyExchangeRateDataService currencyExchangeRateDataService;
    private final TransactionService transactionService;

    @Autowired
    public CardPaymentProcessingService(MerchantService merchantService,
                                        CardService cardService,
                                        CurrencyDataService currencyDataService,
                                        CurrencyExchangeRateDataService currencyExchangeRateDataService,
                                        TransactionService transactionService) {

        this.merchantService = merchantService;
        this.cardService = cardService;
        this.currencyDataService = currencyDataService;
        this.currencyExchangeRateDataService = currencyExchangeRateDataService;
        this.transactionService = transactionService;
    }

    @Override
    public AuthorizePaymentResponse authorizePayment(AuthorizePaymentRequest request) {
        AuthorizePaymentResponse response;
        try {
            // get db data
            Card card = cardService.getCardByHash(request.getCardIdentifier());
            validateCardStatus(card.getStatus());
            Merchant merchant = merchantService.getOrCreateMerchant(request);
            Currency currency = currencyDataService.getCurrencyByCode(request.getCurrency());
            // validate request
            Wallet wallet = getWalletForCard(card.getAccount(), request.getCurrency());
            BigDecimal amount = getAmount(wallet, request); // FX conversion if needed
            validateBalance(wallet, amount);
            AuthPaymentContext ctx = new AuthPaymentContext(card, merchant, wallet, amount, currency);
            // place hold
            String authCode = transactionService.placeAuthorizationHold(ctx);
            response = buildSuccessAuthorizationResponse(ctx, authCode);

        } catch (Exception ex) {
            log.error("Authorization hold request failed. {}", request);
            response = buildFailedAuthorizationResponse(request, ex.getMessage());
        }
        return response;
    }

    private Wallet getWalletForCard(Account account, String currencyCode) {
        return account.getWalletByCurrencyCode(currencyCode)
                .orElseGet(() -> account.getMainWallet()
                        .orElseThrow(() -> new IllegalArgumentException(String.format("No wallet found for account: %s", account.getAccNumber()))));
    }

    private BigDecimal getAmount(Wallet wallet, AuthorizePaymentRequest authorizePaymentRequest) {
        return wallet.getCurrency().getCurrencyCode().equals(authorizePaymentRequest.getCurrency())
                ? BigDecimal.valueOf(authorizePaymentRequest.getAmount())
                : currencyExchangeRateDataService.convert(BigDecimal.valueOf(authorizePaymentRequest.getAmount()), wallet.getCurrency().getCurrencyCode(), authorizePaymentRequest.getCurrency());
    }

    private void validateBalance(Wallet wallet, BigDecimal amount) {
        // TODO: check for all current unreleased holds
        if (!wallet.hasAvailableBalance(amount)) {
            throw new IllegalArgumentException(String.format("Insufficient funds. Request: %s, Available Balance: %s", amount, wallet.getBalance()));
        }
    }

    private void validateCardStatus(CardStatus cardStatus) {
        if (!cardStatus.equals(CardStatus.ACTIVE)) {
            throw new IllegalArgumentException(String.format("Invalid card status: %s", cardStatus.name()));
        }
    }

    private AuthorizePaymentResponse buildSuccessAuthorizationResponse(AuthPaymentContext ctx, String authCode) {
        AuthorizePaymentResponse response = new AuthorizePaymentResponse();
        response.setStatus(RequestResultStatus.SUCCESS);
        response.setMessage(String.format("Payment to %s Authorized", ctx.merchant().getMerchantName()));
        response.setMerchant(ctx.merchant().getMerchantName());
        response.setCurrency(ctx.wallet().getCurrency().getCurrencyCode());
        response.setAccountNumber(ctx.wallet().getAccount().getAccountDetails().getAccountNumber());
        response.setPrice(ctx.authorizedAmount().doubleValue());

        response.setAuthCode(authCode);

        return response;
    }

    private AuthorizePaymentResponse buildFailedAuthorizationResponse(AuthorizePaymentRequest request, String errorMessage) {
        AuthorizePaymentResponse response = new AuthorizePaymentResponse();
        response.setStatus(RequestResultStatus.FAILED);
        response.setMessage(String.format("Failed to authorize payment to %s. Error: [%s]", request.getMerchant(), errorMessage));
        response.setMerchant(request.getMerchant());
        response.setCurrency(request.getCurrency());
        response.setPrice(request.getAmount());

        response.setAuthCode(AuthCodeType.UNAUTHORIZED.name());

        return response;
    }


}
