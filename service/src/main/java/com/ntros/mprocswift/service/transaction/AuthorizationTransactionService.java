package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.TransactionStatus;
import com.ntros.mprocswift.model.transactions.TransactionType;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import com.ntros.mprocswift.repository.transaction.TransactionRepository;
import com.ntros.mprocswift.repository.transaction.TransactionStatusRepository;
import com.ntros.mprocswift.repository.transaction.TransactionTypeRepository;
import com.ntros.mprocswift.repository.transaction.card.AuthorizedHoldRepository;
import com.ntros.mprocswift.repository.transaction.card.CardAuthorizationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthorizationTransactionService implements TransactionService {

    private static final String DEFAULT_AUTH_STATUS = "AUTHORIZED";
    private static final String DEFAULT_AUTH_TYPE = "AUTHORIZED_HOLD";

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final CardAuthorizationRepository cardAuthorizationRepository;
    private final AuthorizedHoldRepository authorizedHoldRepository;


    @Autowired
    public AuthorizationTransactionService(
            TransactionRepository transactionRepository,
            TransactionTypeRepository transactionTypeRepository,
            TransactionStatusRepository transactionStatusRepository,
            CardAuthorizationRepository cardAuthorizationRepository,
            AuthorizedHoldRepository authorizedHoldRepository) {

        this.transactionRepository = transactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        this.cardAuthorizationRepository = cardAuthorizationRepository;
        this.authorizedHoldRepository = authorizedHoldRepository;
    }

    @Transactional
    @Modifying
    @Override
    public String placeAuthorizationHold(AuthPaymentContext authPaymentContext) {
        Transaction base = TransactionFactory.buildAuthBaseTransaction(authPaymentContext);
        TransactionStatus status = transactionStatusRepository.findByStatusName(DEFAULT_AUTH_STATUS).orElseThrow(() -> new NotFoundException(String.format("TX Status not found: %s", DEFAULT_AUTH_STATUS)));
        TransactionType type = transactionTypeRepository.findByTypeName(DEFAULT_AUTH_TYPE).orElseThrow(() -> new NotFoundException(String.format("TX Type not found: %s", DEFAULT_AUTH_TYPE)));
        base.setStatus(status);
        base.setType(type);

        transactionRepository.save(base);

        CardAuthorization authorization = TransactionFactory.buildCardAuthorization(base, authPaymentContext);
        cardAuthorizationRepository.save(authorization);

        AuthorizedHold hold = TransactionFactory.buildAuthorizedHold(authorization, authPaymentContext);
        authorizedHoldRepository.save(hold);

        log.info("Created hold: {}", hold);

        return authorization.getAuthorizationCode();
    }

}
