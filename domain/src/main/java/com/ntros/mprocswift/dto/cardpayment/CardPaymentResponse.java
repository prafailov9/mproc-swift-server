package com.ntros.mprocswift.dto.cardpayment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
public class CardPaymentResponse {

    private boolean success;
    private String message;

}
