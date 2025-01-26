package com.ntros.mprocswift.exceptions;

public class CardPaymentFailedException extends RuntimeException {

    public CardPaymentFailedException(String message) {
        super(message);
    }

    public CardPaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
