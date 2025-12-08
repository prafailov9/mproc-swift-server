package com.ntros.mprocswift.dto.cardpayment;

import lombok.Data;

@Data
public class HoldSettlementResponse {

    private boolean success;
    private String description;

    private String settledAmount;
    private String currencyCode;
    private String merchant;
}
