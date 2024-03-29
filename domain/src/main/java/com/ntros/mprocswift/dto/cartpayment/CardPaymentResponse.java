package com.ntros.mprocswift.dto.cartpayment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentResponse {

    private boolean success;
    private String message;

}
