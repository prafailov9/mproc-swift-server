package com.ntros.mprocswift.exceptions;

public class AddressHashingFailedException extends RuntimeException {

    private static final String MESSAGE = "Failed to generate addres hash for address: %s";

    public AddressHashingFailedException(String addressString, Throwable cause) {
        super(String.format(MESSAGE, addressString), cause);
    }

}
