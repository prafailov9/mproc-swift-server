package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.exceptions.NotFoundException;
import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.ledger.LedgerAccount;
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
import com.ntros.mprocswift.service.ledger.LedgerAccountService;
import com.ntros.mprocswift.service.ledger.LedgerEntryService;
import com.ntros.mprocswift.service.ledger.Posting;
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
  private final LedgerAccountService ledgerAccountService;
  private final LedgerEntryService ledgerEntryService;

  @Autowired
  public AuthorizationTransactionService(
      TransactionRepository transactionRepository,
      TransactionTypeRepository transactionTypeRepository,
      TransactionStatusRepository transactionStatusRepository,
      CardAuthorizationRepository cardAuthorizationRepository,
      AuthorizedHoldRepository authorizedHoldRepository,
      HoldSettlementRepository holdSettlementRepository,
      LedgerAccountService ledgerAccountService,
      LedgerEntryService ledgerEntryService) {

    this.transactionRepository = transactionRepository;
    this.transactionTypeRepository = transactionTypeRepository;
    this.transactionStatusRepository = transactionStatusRepository;
    this.cardAuthorizationRepository = cardAuthorizationRepository;
    this.authorizedHoldRepository = authorizedHoldRepository;
    this.holdSettlementRepository = holdSettlementRepository;
    this.ledgerAccountService = ledgerAccountService;
    this.ledgerEntryService = ledgerEntryService;
  }

  @Transactional
  @Override
  public String placeHold(AuthPaymentContext authPaymentContext) {
    Transaction baseTx = buildAuthBaseTransaction(authPaymentContext);
    TransactionStatus status =
        transactionStatusRepository
            .findByStatusName(DEFAULT_AUTH_STATUS)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("TX Status not found: %s", DEFAULT_AUTH_STATUS)));
    TransactionType type =
        transactionTypeRepository
            .findByTypeName(DEFAULT_AUTH_TYPE)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("TX Type not found: %s", DEFAULT_AUTH_TYPE)));

    baseTx.setStatus(status);
    baseTx.setType(type);
    transactionRepository.save(baseTx);

    CardAuthorization authorization = buildCardAuthorization(baseTx, authPaymentContext);
    cardAuthorizationRepository.save(authorization);

    AuthorizedHold hold = buildAuthorizedHold(authorization, authPaymentContext);
    authorizedHoldRepository.save(hold);

    log.info("Created hold: {}", hold);

    LedgerAccount available =
        ledgerAccountService.getAvailableForWallet(authPaymentContext.wallet());
    LedgerAccount held = ledgerAccountService.getHeldForWallet(authPaymentContext.wallet());

    String entryGroupKey = "CAH:" + baseTx.getTransactionId();

    BigDecimal reserved = authPaymentContext.authorizedAmount(); // should be wallet currency amount

    // take money from available, add to held
    ledgerEntryService.createLedgerEntries(
        baseTx,
        List.of(
            new Posting(
                held, available, reserved, "Card Auth Hold Payment Reservation", entryGroupKey)));

    return authorization.getAuthorizationCode();
  }

  @Transactional
  @Override
  public HoldSettlement settleHold(
      CardAuthorization cardAuth,
      AuthorizedHold authorizedHold,
      AuthPaymentContext authPaymentContext) {

    // validate current status (authorization tx)
    Transaction authTx = cardAuth.getTransaction();
    if (!authTx.getStatus().getStatusName().equals(DEFAULT_AUTH_STATUS)) {
      throw new IllegalArgumentException("tx status is not authorized.");
    }

    // update authorization status -> SETTLED
    TransactionStatus settledStatus =
        transactionStatusRepository
            .findByStatusName(TX_AUTH_SETTLED_STATUS)
            .orElseThrow(
                () -> new NotFoundException("TX Status not found: " + TX_AUTH_SETTLED_STATUS));

    authTx.setStatus(settledStatus);
    transactionRepository.saveAndFlush(authTx);

    // create settlement transaction (NEW ROW)
    Transaction settlementTx =
        TransactionFactory.buildSettlementTransaction(cardAuth, authPaymentContext);

    TransactionType settlementType =
        transactionTypeRepository
            .findByTypeName("CARD_SETTLEMENT")
            .orElseThrow(() -> new NotFoundException("TX Type not found: CARD_SETTLEMENT"));

    TransactionStatus completed =
        transactionStatusRepository
            .findByStatusName("COMPLETED")
            .orElseThrow(() -> new NotFoundException("TX Status not found: COMPLETED"));

    settlementTx.setType(settlementType);
    settlementTx.setStatus(completed);

    transactionRepository.saveAndFlush(settlementTx);

    // create hold_settlement row pointing to settlementTx
    HoldSettlement savedSettlement =
        holdSettlementRepository.save(
            TransactionFactory.buildHoldSettlement(settlementTx, cardAuth, authPaymentContext));

    // mark hold released
    authorizedHold.setIsReleased(true);
    authorizedHold.setReleasedAt(OffsetDateTime.now());
    authorizedHoldRepository.save(authorizedHold);

    // ledger posting must point to the settlementTx
    LedgerAccount held = ledgerAccountService.getHeldForWallet(authPaymentContext.wallet());
    LedgerAccount merchantSettlement =
        ledgerAccountService.getOrCreateMerchantSettlementAccount(
            authPaymentContext.merchant(), authPaymentContext.wallet().getCurrency());

    String entryGroupKey = "CSH:" + settlementTx.getTransactionId();
    String description =
        "Merchant " + authPaymentContext.merchant().getMerchantName() + " settlement";

    ledgerEntryService.createLedgerEntries(
        settlementTx,
        List.of(
            new Posting(
                merchantSettlement,
                held,
                authorizedHold.getHoldAmount(),
                description,
                entryGroupKey)));

    return savedSettlement;
  }

  @Override
  public CardAuthorization getCardAuthorization(String authCode) {
    CardAuthorization cardAuthorization =
        cardAuthorizationRepository
            .findOneByAuthorizationCode(authCode)
            .orElseThrow(
                () ->
                    new NotFoundException(String.format("Auth not found for code: %s", authCode)));
    log.info("Found Card Auth: {}", cardAuthorization);
    return cardAuthorization;
  }

  @Override
  public AuthorizedHold getAuthorizedHold(String authCode) {
    AuthorizedHold hold =
        authorizedHoldRepository
            .findOneByAuthCode(authCode)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Auth Hold not found for code: %s", authCode)));
    log.info("Found Auth Hold: {}", hold);
    return hold;
  }

  @Override
  public BigDecimal getHoldAmountSumForWallet(Wallet wallet) {
    List<AuthorizedHold> holds =
        authorizedHoldRepository.findAllByWalletAndIsReleasedFalseAndExpiresAtAfter(
            wallet, OffsetDateTime.now());
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
