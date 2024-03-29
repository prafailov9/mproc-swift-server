package com.ntros.mprocswift.dto.transfer;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public abstract class TransferResponse {

    protected String status;
    protected TransferRequest transferRequest;

}
