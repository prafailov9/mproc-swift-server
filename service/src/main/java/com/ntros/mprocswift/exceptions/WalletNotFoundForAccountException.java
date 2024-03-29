package com.ntros.mprocswift.exceptions;

public class WalletNotFoundForAccountException extends RuntimeException {

    public WalletNotFoundForAccountException(int accountId, Throwable cause) {
        super(String.format("Wallet not found for accountId: %s", accountId), cause);
    }
}
