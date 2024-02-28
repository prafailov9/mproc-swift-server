package com.ntros.mprocswift.exceptions;

public class WalletDeleteFailedException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Failed to delete %s wallet for account:[%s]\n";

    public WalletDeleteFailedException(String currencyCode, String accountNumber) {
        super(String.format(MESSAGE_TEMPLATE, currencyCode, accountNumber));
    }

    public WalletDeleteFailedException(String currencyCode, String accountNumber, Throwable cause) {
        super(String.format(MESSAGE_TEMPLATE, currencyCode, accountNumber) + cause.getMessage(), cause);
    }

}
