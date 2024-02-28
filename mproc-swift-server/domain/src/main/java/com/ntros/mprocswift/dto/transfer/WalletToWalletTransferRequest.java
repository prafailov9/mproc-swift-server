package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.validation.CurrenciesNotEqual;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.
@CurrenciesNotEqual
public class WalletToWalletTransferRequest extends TransferRequest {

    @NotNull(message = "currency code cannot be null.")
    @NotBlank(message = "currency code cannot be empty.")
    private String toCurrencyCode; // recipient wallet based on its currency

}
