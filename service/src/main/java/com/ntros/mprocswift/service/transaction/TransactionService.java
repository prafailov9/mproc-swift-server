package com.ntros.mprocswift.service.transaction;

import com.ntros.mprocswift.model.transactions.card.AuthorizedHold;

public interface TransactionService {

    String placeAuthorizationHold(AuthPaymentContext authPaymentContext);

}
