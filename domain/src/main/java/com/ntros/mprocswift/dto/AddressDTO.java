package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AddressDTO {

    private String country;
    private String city;
    private String streetName;
    private String streetNumber;
    private String postalCode;

}
