package com.ntros.mprocswift.model.account;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class ExternalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer externalAccountId;

    @OneToOne
    @JoinColumn(name = "account_details_id")
    private AccountDetails accountDetails;

}
