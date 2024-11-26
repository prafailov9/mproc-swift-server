package com.ntros.mprocswift.exceptions;

import com.ntros.mprocswift.model.Merchant;

public class MerchantConstraintFailureException extends RuntimeException {

    public MerchantConstraintFailureException(Merchant merchant) {
        super(String.format("Failed to persist Merchant: %s", merchant));

    }

}
