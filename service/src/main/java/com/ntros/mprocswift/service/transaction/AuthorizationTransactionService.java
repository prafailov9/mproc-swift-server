package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import com.ntros.mprocswift.model.transactions.card.HoldSettlement;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.repository.transaction.card.AuthorizedHoldRepository;
import com.ntros.mprocswift.repository.transaction.card.CardAuthorizationRepository;
import com.ntros.mprocswift.repository.transaction.card.HoldSettlementRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ntros.mprocswift.service.transaction.TransactionFactory.*;

@Service
@Slf4j
public class AuthorizationTransactionService implements TransactionService {

    private static final String DEFAULT_AUTH_STATUS = "AUTHORIZED";
    private static final String TX_AUTH_SETTLED_STATUS = "SETTLED";
    private static final String DEFAULT_AUTH_TYPE = "AUTHORIZED_HOLD";

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final CardAuthorizationRepository cardAuthorizationRepository;
    private final AuthorizedHoldRepository authorizedHoldRepository;
    private final HoldSettlementRepository holdSettlementRepository;


    @Autowired
    public AuthorizationTransactionService(TransactionRepository transactionRepository, TransactionTypeRepository transactionTypeRepository, TransactionStatusRepository transactionStatusRepository, CardAuthorizationRepository cardAuthorizationRepository, AuthorizedHoldRepository authorizedHoldRepository, HoldSettlementRepository holdSettlementRepository) {

        this.transactionRepository = transactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        this.cardAuthorizationRepository = cardAuthorizationRepository;
        this.authorizedHoldRepository = authorizedHoldRepository;
        this.holdSettlementRepository = holdSettlementRepository;
    }

    @Transactional
    @Override
    public String placeHold(AuthPaymentContext authPaymentContext) {
        Transaction base = buildAuthBaseTransaction(authPaymentContext);
        TransactionStatus status = transactionStatusRepository.findByStatusName(DEFAULT_AUTH_STATUS).orElseThrow(() -> new NotFoundException(String.format("TX Status not found: %s", DEFAULT_AUTH_STATUS)));
        TransactionType type = transactionTypeRepository.findByTypeName(DEFAULT_AUTH_TYPE).orElseThrow(() -> new NotFoundException(String.format("TX Type not found: %s", DEFAULT_AUTH_TYPE)));

        base.setStatus(status);
        base.setType(type);
        transactionRepository.save(base);

        CardAuthorization authorization = buildCardAuthorization(base, authPaymentContext);
        cardAuthorizationRepository.save(authorization);

        AuthorizedHold hold = buildAuthorizedHold(authorization, authPaymentContext);
        authorizedHoldRepository.save(hold);

        log.info("Created hold: {}", hold);

        return authorization.getAuthorizationCode();
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public HoldSettlement settleHold(CardAuthorization cardAuth, AuthorizedHold authorizedHold, AuthPaymentContext authPaymentContext) {
        // validate current status
        Transaction baseTx = cardAuth.getTransaction();
        TransactionStatus status = baseTx.getStatus();
        if (!status.getStatusName().equals(DEFAULT_AUTH_STATUS)) {
            throw new IllegalArgumentException("tx status is not authorized.");
        }
        // update tx status
        TransactionStatus settledStatus = transactionStatusRepository.findByStatusName("SETTLED").orElseThrow(() -> new NotFoundException(String.format("TX Status not found: %s", TX_AUTH_SETTLED_STATUS)));
        baseTx.setStatus(settledStatus);
        transactionRepository.save(baseTx);

        // create settlement
        HoldSettlement savedSettlement = holdSettlementRepository.save(buildHoldSettlement(cardAuth, authPaymentContext));
        log.info("Created settlement TX: {}", savedSettlement);

        // mark hold as released
        authorizedHold.setIsReleased(true);
        authorizedHold.setReleasedAt(OffsetDateTime.now());
        authorizedHoldRepository.save(authorizedHold);

        return savedSettlement;
    }

    @Override
    public CardAuthorization getCardAuthorization(String authCode) {
        CardAuthorization cardAuthorization = cardAuthorizationRepository.findOneByAuthorizationCode(authCode).orElseThrow(() -> new NotFoundException(String.format("Auth not found for code: %s", authCode)));
        log.info("Found Card Auth: {}", cardAuthorization);
        return cardAuthorization;
    }

    @Override
    public AuthorizedHold getAuthorizedHold(String authCode) {
        AuthorizedHold hold = authorizedHoldRepository.findOneByAuthCode(authCode).orElseThrow(() -> new NotFoundException(String.format("Auth Hold not found for code: %s", authCode)));
        log.info("Found Auth Hold: {}", hold);
        return hold;
    }

    @Override
    public BigDecimal getHoldAmountSumForWallet(Wallet wallet) {
        List<AuthorizedHold> holds = authorizedHoldRepository.findAllByWalletAndIsReleasedFalseAndExpiresAtAfter(wallet, OffsetDateTime.now());
        if (holds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (var hold : holds) {
            sum = sum.add(hold.getHoldAmount());
        }
        return sum;
    }

    @Override
    public HoldSettlement getHoldSettlement(String authCode) {
        return holdSettlementRepository.findOneByAuthCode(authCode).orElse(null);
    }

}
