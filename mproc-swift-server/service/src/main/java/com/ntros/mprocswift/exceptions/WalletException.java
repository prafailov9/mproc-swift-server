package com.ntros.mprocswift.exceptions;

public class WalletException extends RuntimeException {


    public WalletException(final String message) {
        super(message);
    }
    public WalletException(final String message, final Throwable cause) {
        super(message, cause);
    }



}
