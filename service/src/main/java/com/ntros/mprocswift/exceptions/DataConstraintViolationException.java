package com.ntros.mprocswift.exceptions;

public class DataConstraintViolationException extends RuntimeException {

    public DataConstraintViolationException(String message) {
        super(message);
    }

    public DataConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
