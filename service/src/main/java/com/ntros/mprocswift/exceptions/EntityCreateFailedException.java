package com.ntros.mprocswift.exceptions;

public class EntityCreateFailedException extends RuntimeException {

    public EntityCreateFailedException(String message) {
        super(message);
    }

    public EntityCreateFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}


