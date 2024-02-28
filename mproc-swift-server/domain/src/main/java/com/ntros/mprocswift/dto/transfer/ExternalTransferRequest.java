package com.ntros.mprocswift.dto.transfer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.
public class ExternalTransferRequest extends TransferRequest {

    private String recipientName;
    // bank details
    private String iban;
    private String bicswift;
    private String country;
}
