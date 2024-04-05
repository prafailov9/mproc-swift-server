package com.ntros.mprocswift.exceptions;

public class CardNotCreatedException extends RuntimeException {

    public CardNotCreatedException(String message) {
        super(message);
    }

    public CardNotCreatedException(String message, Throwable cause) {
        super(message, cause);
    }

}
