package com.ntros.mprocswift.exceptions;

public class CurrencyNotSupportedException extends RuntimeException {

    public CurrencyNotSupportedException(String currency) {
        super(String.format("%s is not supported.", currency));
    }

}
