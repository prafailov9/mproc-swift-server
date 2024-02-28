package com.ntros.mprocswift.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer addressId;
    @Column(nullable = false)
    private String addressHash;

    private String country;
    private String city;
    private String streetName;
    private String streetNumber;
    private String postalCode;


}
