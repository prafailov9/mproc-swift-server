package com.ntros.mprocswift.service.payment;

import com.ntros.mprocswift.converter.MerchantConverter;
import com.ntros.mprocswift.dto.cardpayment.CardPaymentRequest;
import com.ntros.mprocswift.dto.cardpayment.CardPaymentResponse;
import com.ntros.mprocswift.exceptions.CardPaymentFailedException;
import com.ntros.mprocswift.exceptions.InsufficientFundsException;
import com.ntros.mprocswift.model.Merchant;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.card.Card;
import com.ntros.mprocswift.model.currency.Currency;
import com.ntros.mprocswift.model.transactions.CardPayment;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.repository.transaction.CardPaymentRepository;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.service.AbstractService;
import com.ntros.mprocswift.service.card.CardService;
import com.ntros.mprocswift.service.merchant.MerchantService;
import com.ntros.mprocswift.service.wallet.WalletService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.math.BigDecimal.valueOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Service
@Transactional
@Slf4j
public class CardPaymentProcessingService extends AbstractService implements PaymentService {

    private final MerchantService merchantService;
    private final CardService cardService;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final CardPaymentRepository cardPaymentRepository;
    private final MerchantConverter merchantConverter;

    @Autowired
    public CardPaymentProcessingService(MerchantService merchantService,
                                        CardService cardService,
                                        WalletService walletService,
                                        TransactionRepository transactionRepository,
                                        CardPaymentRepository cardPaymentRepository,
                                        MerchantConverter merchantConverter) {

        this.merchantService = merchantService;
        this.cardService = cardService;
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
        this.cardPaymentRepository = cardPaymentRepository;
        this.merchantConverter = merchantConverter;
    }

    @Override
    public CompletableFuture<CardPaymentResponse> processPayment(CardPaymentRequest cardPaymentRequest) {
        CompletableFuture<Card> cardFuture = cardService
                .getCard(cardPaymentRequest.getCardDTO());

        CompletableFuture<Merchant> merchantFuture = findOrCreateMerchant(cardPaymentRequest);

        return cardFuture.thenCombineAsync(merchantFuture, (card, merchant) ->
                        walletService.getWalletByCurrencyCodeAndAccountId(cardPaymentRequest.getCurrency(), card.getAccount().getAccountId())
                                .thenComposeAsync(wallet -> {
                                    makePayment(wallet, card, merchant, cardPaymentRequest);
                                    return buildCardPaymentResponse(merchant, wallet, cardPaymentRequest.getPrice());
                                }, executor).exceptionally(ex -> {
                                    log.error("Unable to process payment. {}", ex.getCause().getMessage(), ex.getCause());
                                    throw new CardPaymentFailedException(ex.getCause().getMessage(), ex.getCause());
                                }))
                .thenCompose(response -> response);
    }

    private void makePayment(Wallet wallet, Card card, Merchant merchant, CardPaymentRequest cardPaymentRequest) {
        BigDecimal price = valueOf(cardPaymentRequest.getPrice());
        validateBalance(wallet.getAccount().getAccNumber(), wallet.getBalance(), price);
        wallet.decreaseBalance(price);

        walletService.updateBalance(wallet.getWalletId(), wallet.getBalance());
        createPaymentTransaction(cardPaymentRequest, wallet.getCurrency(), card, merchant);
    }

    private CompletableFuture<Merchant> findOrCreateMerchant(CardPaymentRequest cardPaymentRequest) {
        return merchantService.getMerchantByName(cardPaymentRequest.getMerchantDTO().getMerchantName())
                .thenComposeAsync(
                        merchant ->
                                (merchant == null)
                                        ? merchantService.createMerchant(merchantConverter.toModel(cardPaymentRequest.getMerchantDTO()))
                                        : completedFuture(merchant)
                        , executor);
    }

    @Transactional
    @Modifying
    private void createPaymentTransaction(CardPaymentRequest cardPaymentRequest, Currency currency, Card card, Merchant merchant) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(OffsetDateTime.now());
        transaction.setType(TransactionType.CARD_PAYMENT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription("");
        transaction.setAmount(valueOf(cardPaymentRequest.getPrice()));
        transaction.setCurrency(currency);
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        CardPayment cardPayment = new CardPayment();
        cardPayment.setTransactionId(savedTransaction.getTransactionId());
        cardPayment.setTransaction(savedTransaction);
        cardPayment.setCard(card);
        cardPayment.setMerchant(merchant);
        cardPaymentRepository.save(cardPayment);
    }

    private CompletableFuture<CardPaymentResponse> buildCardPaymentResponse(Merchant merchant, Wallet wallet, double price) {
        return supplyAsync(() -> {
            CardPaymentResponse response = new CardPaymentResponse();
            response.setStatus("success");
            response.setMessage("successful payment to " + merchant.getMerchantName());
            response.setMerchant(merchant.getMerchantName());
            response.setCurrency(wallet.getCurrency().getCurrencyCode());
            response.setAccountNumber(wallet.getAccount().getAccountDetails().getAccountNumber());
            response.setPrice(price);
            return response;
        }, executor);
    }

    private void validateBalance(String accountNumber, BigDecimal balance, BigDecimal price) {
        if (balance.compareTo(price) < 0) {
            throw new InsufficientFundsException(format("Not enough funds for this purchase. " +
                            "Account %s with balance: %s, product price: %s",
                    accountNumber, balance, price));
        }
    }


}
