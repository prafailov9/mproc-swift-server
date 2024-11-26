package com.ntros.mprocswift.dto.cardpayment;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CardPaymentRequest {

    @NotBlank(message = "Must have Merchant name.")
    private String merchantName;

    @Size(min = 5, message = "product price cannot be less than 5.")
    private Double price;

    @NotBlank(message = "Must have currency.")
    private String currency;

    private String provider;
    private String cardNumber;
    private String expirationDate;
    private String cvv;

    private String merchantCategoryCode;
    private String merchantIdentifierCode;

    private String merchantContactDetails;

}
