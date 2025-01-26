package com.ntros.mprocswift.dto.cardpayment;

import com.ntros.mprocswift.dto.CardDTO;
import com.ntros.mprocswift.dto.MerchantDTO;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CardPaymentRequest {


    @Size(min = 5, message = "product price cannot be less than 5.")
    private Double price;

    @NotBlank(message = "Must have currency.")
    private String currency;

    private CardDTO cardDTO;
    private MerchantDTO merchantDTO;
}
