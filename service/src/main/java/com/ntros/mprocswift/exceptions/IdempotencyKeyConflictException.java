package com.ntros.mprocswift.exceptions;

public class IdempotencyKeyConflictException extends RuntimeException{

    public IdempotencyKeyConflictException(String message) {
        super(message);
    }

    public IdempotencyKeyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
