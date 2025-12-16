package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.Wallet;
import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;
import com.ntros.mprocswift.model.transactions.card.CardAuthorization;
import com.ntros.mprocswift.model.transactions.card.HoldSettlement;

import java.math.BigDecimal;

public interface TransactionService {

    String placeHold(AuthPaymentContext authPaymentContext);

    CardAuthorization getCardAuthorization(String authCode);

    AuthorizedHold getAuthorizedHold(String authCode);

    long getHoldAmountSumForWallet(Wallet wallet);

    HoldSettlement getHoldSettlement(String authCode);

    HoldSettlement settleHold(CardAuthorization cardAuth, AuthorizedHold authorizedHold, AuthPaymentContext authPaymentContext);
}
