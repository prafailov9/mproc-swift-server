package com.ntros.mprocswift.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer merchantId;

    public Merchant(String merchantName) {
        this.merchantName = merchantName;
    }

    private String merchantName;
    private String merchantCategoryCode;
    @Column(name = "mid")
    private String merchantIdentifierCode;

    private String contactDetails;
}
