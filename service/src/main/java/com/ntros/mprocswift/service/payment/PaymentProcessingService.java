package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.AuthCodeType;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.AuthorizePaymentResponse;
import com.ntros.mprocswift.dto.cardpayment.RequestResultStatus;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.service.card.CardService;
import com.ntros.mprocswift.service.currency.CurrencyDataService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateDataService;
import com.ntros.mprocswift.service.merchant.MerchantService;
import com.ntros.mprocswift.service.transaction.AuthPaymentContext;
import com.ntros.mprocswift.service.transaction.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

import static com.ntros.mprocswift.utils.TextFormatter.format;

@Service
@Slf4j
public class PaymentProcessingService implements PaymentService {

    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;
    private final MerchantService merchantService;
    private final CardService cardService;
    private final CurrencyDataService currencyDataService;
    private final CurrencyExchangeRateDataService currencyExchangeRateDataService;
    private final TransactionService transactionService;

    @Autowired
    public PaymentProcessingService(MerchantService merchantService,
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
            Merchant merchant = merchantService.getOrCreateMerchant(request);
            Currency currency = currencyDataService.getCurrencyByCode(request.getCurrency());

            // validate request
            Wallet wallet = card.getAccount()
                    .getWalletByCurrencyCode(request.getCurrency())
                    .orElseThrow(() -> new IllegalArgumentException(format("No wallet found for account: %s", card.getAccount().getAccNumber())));
            BigDecimal amount = getAmount(wallet, request); // FX conversion if needed
            // TODO: check for all current unreleased holds
            if (!wallet.hasAvailableBalance(amount)) {
                throw new IllegalArgumentException(format("Insufficient funds. Request: %s, Available Balance: %s", amount, wallet.getBalance()));
            }
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

    private BigDecimal getAmount(Wallet wallet, AuthorizePaymentRequest authorizePaymentRequest) {
        return wallet.getCurrency().getCurrencyCode().equals(authorizePaymentRequest.getCurrency())
                ? BigDecimal.valueOf(authorizePaymentRequest.getAmount())
                : currencyExchangeRateDataService.convert(BigDecimal.valueOf(authorizePaymentRequest.getAmount()), wallet.getCurrency().getCurrencyCode(), authorizePaymentRequest.getCurrency());
    }

    private AuthorizePaymentResponse buildSuccessAuthorizationResponse(AuthPaymentContext ctx, String authCode) {
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

    private AuthorizePaymentResponse buildFailedAuthorizationResponse(AuthorizePaymentRequest request, String errorMessage) {
        AuthorizePaymentResponse response = new AuthorizePaymentResponse();
        response.setStatus(RequestResultStatus.FAILED);
        response.setMessage(format("Failed to authorize payment to %s. Error: [%s]", request.getMerchant(), errorMessage));
        response.setMerchant(request.getMerchant());
        response.setCurrency(request.getCurrency());
        response.setPrice(request.getAmount());

        response.setAuthCode(AuthCodeType.UNAUTHORIZED.name());

        return response;
    }
}
