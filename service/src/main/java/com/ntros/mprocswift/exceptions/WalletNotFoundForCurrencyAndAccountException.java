package com.ntros.mprocswift.exceptions;

public class WalletNotFoundForCurrencyAndAccountException extends RuntimeException {

    private static final String MSG = "Wallet not found for currency: %s and account id: %s";


    public WalletNotFoundForCurrencyAndAccountException(String currency, int accountId) {
        super(String.format(MSG, currency, accountId));
    }
}
