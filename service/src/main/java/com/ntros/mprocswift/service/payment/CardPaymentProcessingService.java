package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.CardPaymentResponse;
import com.ntros.mprocswift.exceptions.CannotRefreshCardException;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.account.Account;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.transactions.CardPayment;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.repository.MerchantRepository;
import com.ntros.mprocswift.repository.transaction.CardPaymentRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.service.AbstractService;
import com.ntros.mprocswift.service.card.CardDataService;
import com.ntros.mprocswift.service.currency.CurrencyExchangeRateService;
import com.ntros.mprocswift.service.merchant.MerchantService;
import com.ntros.mprocswift.service.wallet.WalletService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Transactional
@Slf4j
public class CardPaymentProcessingService extends AbstractService implements PaymentService {


    private final MerchantService merchantService;
    private final CardDataService cardDataService;
    private final WalletService walletService;

    private final TransactionRepository transactionRepository;
    private final CardPaymentRepository cardPaymentRepository;
    private final CurrencyExchangeRateService currencyExchangeRateService;

    @Autowired
    public CardPaymentProcessingService(MerchantService merchantService, CardDataService cardDataService, WalletService walletService, CurrencyExchangeRateService currencyExchangeRateService, TransactionRepository transactionRepository, CardPaymentRepository cardPaymentRepository) {

        this.merchantService = merchantService;
        this.cardDataService = cardDataService;
        this.walletService = walletService;
        this.currencyExchangeRateService = currencyExchangeRateService;
        this.transactionRepository = transactionRepository;
        this.cardPaymentRepository = cardPaymentRepository;
    }

    @Override
    public CompletableFuture<CardPaymentResponse> processPayment(CardPaymentRequest cardPaymentRequest) {
        CompletableFuture<Card> cardFuture = cardDataService
                .getCard(cardPaymentRequest.getProvider(),
                        cardPaymentRequest.getCardNumber(),
                        cardPaymentRequest.getExpirationDate(),
                        cardPaymentRequest.getCvv());

        CompletableFuture<Merchant> merchantFuture = findOrCreateMerchant(cardPaymentRequest);

        return cardFuture.thenCombineAsync(merchantFuture, (card, merchant) ->
                walletService.getWalletByCurrencyCodeAndAccountId(cardPaymentRequest.getCurrency(),
                                card.getAccount().getAccountId())
                        .thenApplyAsync(wallet -> {
                            validateBalance(wallet, BigDecimal.valueOf(cardPaymentRequest.getPrice()));
                            return wallet;
                        }, executor).thenApplyAsync(wallet -> {
                            makePayment(card, wallet, merchant, cardPaymentRequest);
                            return wallet;
                        }, executor).thenComposeAsync(wallet -> createCardPaymentResponse(merchant, wallet, cardPaymentRequest.getPrice())), executor).thenComposeAsync(response -> response).exceptionally(ex -> {
            throw new CannotRefreshCardException(ex.getMessage());
        });
    }

    private CompletableFuture<Void> makePayment(Card card, Wallet wallet, Merchant merchant, CardPaymentRequest cardPaymentRequest) {
        return CompletableFuture.supplyAsync(() -> {
                    // deduct price from balance
                    wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(cardPaymentRequest.getPrice())));
                    // update new balance
                    walletService.updateBalance(wallet.getWalletId(), wallet.getBalance());
                    return wallet;
                    // create Payment transaction
                }, executor)
                .thenComposeAsync(v -> createPaymentTransaction(cardPaymentRequest, wallet.getCurrency(), card, merchant),
                        executor);
    }

    private CompletableFuture<Merchant> findOrCreateMerchant(CardPaymentRequest cardPaymentRequest) {
        return merchantService.getMerchantByName(cardPaymentRequest.getMerchantName()).thenComposeAsync(merchant -> {
            if (merchant == null) {
                Merchant m = new Merchant();
                m.setMerchantName(cardPaymentRequest.getMerchantName());
                m.setMerchantIdentifierCode(cardPaymentRequest.getMerchantIdentifierCode());
                m.setMerchantCategoryCode(cardPaymentRequest.getMerchantCategoryCode());
                m.setContactDetails(cardPaymentRequest.getMerchantContactDetails());
                return merchantService.createMerchant(m);
            }
            return CompletableFuture.completedFuture(merchant);

        }, executor).thenApplyAsync(merchant -> merchant);
    }

    @Transactional
    @Modifying
    private CompletableFuture<Void> createPaymentTransaction(CardPaymentRequest cardPaymentRequest, Currency currency, Card card, Merchant merchant) {
        return CompletableFuture.runAsync(() -> {
            Transaction transaction = new Transaction();
            transaction.setTransactionDate(OffsetDateTime.now());
            transaction.setType(TransactionType.CARD_PAYMENT);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setDescription("");
            transaction.setAmount(BigDecimal.valueOf(cardPaymentRequest.getPrice()));
            transaction.setCurrency(currency);
            Transaction saved = transactionRepository.saveAndFlush(transaction);

            CardPayment cardPayment = new CardPayment();
            cardPayment.setTransaction(saved);
            cardPayment.setTransactionId(saved.getTransactionId());
            cardPayment.setCard(card);
            cardPayment.setMerchant(merchant);
            cardPaymentRepository.save(cardPayment);
        }, executor);
    }

    private CompletableFuture<CardPaymentResponse> createCardPaymentResponse(Merchant merchant, Wallet wallet, double price) {
        return CompletableFuture.supplyAsync(() -> {
            CardPaymentResponse response = new CardPaymentResponse();
            response.setStatus("success");
            response.setMessage("success made payment");
            response.setMerchant(merchant.getMerchantName());
            response.setCurrency(wallet.getCurrency().getCurrencyCode());
            response.setAccountNumber(wallet.getAccount().getAccountDetails().getAccountNumber());
            response.setPrice(price);
            return response;
        }, executor);
    }

    private void validateBalance(Wallet wallet, BigDecimal price) {
        if (wallet.getBalance().compareTo(price) >= 0) {
            return;
        }
        Account account = wallet.getAccount();
        validate(account.getTotalBalance(), price, account.getAccountDetails().getAccountNumber());

        account.getWallets().stream()
                .filter(w -> w.equals(wallet))
                .forEach(w -> validate(w.getBalance(), price, account.getAccountDetails().getAccountNumber()));
    }

    private void validate(BigDecimal balance, BigDecimal price, String an) {
        if (balance.compareTo(price) < 0) {
            throw new InsufficientFundsException(String.format("Not enough funds for this purchase. Account %s with balance: %s, product price: %s", an, balance, price));
        }
    }

}
