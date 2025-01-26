package com.ntros.mprocswift.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantDTO {

    @NotBlank(message = "Must have Merchant name.")
    private String merchantName;
    private String merchantCategoryCode;
    private String merchantIdentifierCode;

    private String merchantContactDetails;
}
