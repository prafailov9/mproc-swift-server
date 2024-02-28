package com.ntros.mprocswift.exceptions;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(int addressId) {
        super(String.format("Could not find address with id: %s", addressId));
    }

}
