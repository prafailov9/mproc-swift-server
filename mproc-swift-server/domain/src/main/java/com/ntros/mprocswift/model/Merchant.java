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

    @Column(nullable = false)
    private String merchantName;
    @Column(nullable = false)
    private String merchantCategoryCode;
    @Column(nullable = false)
    private String merchantIdentifierCode;

    private String contactDetails;
}
