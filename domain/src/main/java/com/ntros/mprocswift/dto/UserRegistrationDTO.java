package com.ntros.mprocswift.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Data
@RequiredArgsConstructor
public class UserRegistrationDTO {

    private String firstName;
    private String lastName;

    private String username;
    private String passwordHash;
    private String email;

    private String phoneNumber;
    private OffsetDateTime dateOfBirth;
    private AddressDTO addressDTO;
}
