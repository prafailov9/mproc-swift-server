package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.dto.AddressDTO;
import com.ntros.mprocswift.model.Address;

import java.io.IOException;
import java.net.URISyntaxException;

public interface AddressService {

    Address getAddress(final int addressId);

    Address createAddress(Address address);

    Address createAddress(AddressDTO addressDTO);

    void generateHashedAddressInsertStatements(String sourceFile, String destFile) throws URISyntaxException, IOException;

}
