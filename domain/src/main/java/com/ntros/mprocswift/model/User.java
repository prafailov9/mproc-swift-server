package com.ntros.mprocswift.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"userId", "username", "email"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String email;
    private String phoneNumber;
    private OffsetDateTime dateOfBirth;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;


}
