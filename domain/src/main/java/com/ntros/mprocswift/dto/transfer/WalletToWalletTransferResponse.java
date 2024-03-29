package com.ntros.mprocswift.dto.transfer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.

public class WalletToWalletTransferResponse extends TransferResponse {
}
