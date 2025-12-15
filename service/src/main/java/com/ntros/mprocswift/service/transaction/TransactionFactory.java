package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.transactions.Transaction;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import com.ntros.mprocswift.model.transactions.card.HoldSettlement;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.stream.Collectors;

public final class TransactionFactory {

  public static Transaction buildAuthBaseTransaction(AuthPaymentContext ctx) {
    Transaction transaction = new Transaction();
    transaction.setTransactionDate(OffsetDateTime.now());
    transaction.setDescription("");
    transaction.setAmount(ctx.authorizedAmount()); // reserved amount (wallet currency)
    transaction.setCurrency(ctx.wallet().getCurrency()); // currency matches the amount

    return transaction;
  }

  public static CardAuthorization buildCardAuthorization(
      Transaction baseTransaction, AuthPaymentContext ctx) {
    CardAuthorization auth = new CardAuthorization();
    auth.setTransactionId(baseTransaction.getTransactionId());
    auth.setTransaction(baseTransaction);
    auth.setCard(ctx.card());
    auth.setMerchant(ctx.merchant());

    String authCode =
        String.format(
            "AUTH-%s",
            new Random()
                .ints(8, 0, 36)
                .mapToObj(i -> String.valueOf(Character.toUpperCase(Character.forDigit(i, 36))))
                .collect(Collectors.joining()));

    auth.setAuthorizationCode(authCode);
    auth.setAuthorizedAt(OffsetDateTime.now());

    return auth;
  }

  public static AuthorizedHold buildAuthorizedHold(CardAuthorization auth, AuthPaymentContext ctx) {
    AuthorizedHold hold = new AuthorizedHold();
    hold.setCardAuthorization(auth);
    hold.setWallet(ctx.wallet());
    hold.setHoldAmount(ctx.authorizedAmount());
    hold.setHoldDate(OffsetDateTime.now());
    hold.setExpiresAt(OffsetDateTime.now().plusDays(5));
    hold.setIsReleased(false);

    return hold;
  }

  public static Transaction buildSettlementTransaction(
      CardAuthorization auth, AuthPaymentContext ctx) {
    Transaction tx = new Transaction();
    tx.setTransactionDate(OffsetDateTime.now());
    tx.setDescription("Settlement for " + auth.getAuthorizationCode());
    tx.setAmount(ctx.authorizedAmount()); // wallet currency
    tx.setCurrency(ctx.wallet().getCurrency()); // wallet currency
    tx.setRelatedTransaction(
        auth.getTransaction()); // link to auth tx (use relation if you have it)
    return tx;
  }

  public static HoldSettlement buildHoldSettlement(Transaction settlementTx, CardAuthorization auth, AuthPaymentContext ctx) {
    HoldSettlement settlement = new HoldSettlement();
    settlement.setTransaction(settlementTx); // ✅ new tx
    settlement.setCardAuthorization(auth);
    settlement.setSettledAt(OffsetDateTime.now());
    settlement.setMerchant(ctx.merchant());
    settlement.setCard(ctx.card());
    settlement.setSettledAmount(ctx.authorizedAmount());
    return settlement;
  }

}
