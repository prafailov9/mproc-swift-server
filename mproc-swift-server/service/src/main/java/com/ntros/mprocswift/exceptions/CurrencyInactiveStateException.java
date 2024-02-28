package com.ntros.mprocswift.exceptions;

public class CurrencyInactiveStateException extends RuntimeException {

    public CurrencyInactiveStateException(String code) {
        super(String.format("Currency %s is inactive", code));
    }

}
