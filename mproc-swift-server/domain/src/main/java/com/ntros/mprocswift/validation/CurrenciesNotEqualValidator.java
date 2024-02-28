package com.ntros.mprocswift.validation;

import com.ntros.mprocswift.dto.transfer.WalletToWalletTransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CurrenciesNotEqualValidator implements ConstraintValidator<CurrenciesNotEqual, WalletToWalletTransferRequest> {

    @Override
    public boolean isValid(WalletToWalletTransferRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        } else if (request.getCurrencyCode() == null) {
            return true;
        } else if (request.getToCurrencyCode() == null) {
            return true;
        }
        return !request.getCurrencyCode().equals(request.getToCurrencyCode());
    }
}
