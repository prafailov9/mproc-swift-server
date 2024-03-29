package com.ntros.mprocswift.dto.transfer;

import com.ntros.mprocswift.validation.AccountNumbersNotEqual;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true) // include superclass fields in Lombok's equals and hashCode impls.
@AccountNumbersNotEqual
public class InternalTransferRequest extends TransferRequest {

    @NotNull(message = "recipient AN cannot be null.")
    @NotBlank(message = "recipient AN cannot be empty.")
    @Pattern(regexp = "\\d+", message = "recipient AN must be a number.")
    @Size(min = 8, max = 12, message = "Invalid recipient AN: must be 8 - 12 digits.")
    private String recipientAccountNumber;

}
