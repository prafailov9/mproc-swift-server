package com.ntros.mprocswift.exceptions;

public class ExchangeRateNotFoundForPairException extends RuntimeException {

    public ExchangeRateNotFoundForPairException(String from, String to) {
        super(String.format("Exchange rate not found for: %s/%s", from, to));
    }

}
