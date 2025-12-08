package com.ntros.mprocswift.dto.cardpayment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HoldSettlementRequest {

    @NotBlank(message = "Must have authCode.")
    private String authCode;

}
