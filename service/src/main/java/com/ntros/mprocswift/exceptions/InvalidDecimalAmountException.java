package com.ntros.mprocswift.exceptions;

import java.math.BigDecimal;

public class InvalidDecimalAmountException extends RuntimeException {

    private static final String MESSAGE = "Cannot process empty amount";
    public InvalidDecimalAmountException() {
        super(MESSAGE);
    }
}
