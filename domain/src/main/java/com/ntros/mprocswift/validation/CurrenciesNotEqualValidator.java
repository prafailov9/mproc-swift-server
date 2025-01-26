package com.ntros.mprocswift.validation;

import com.ntros.mprocswift.dto.transfer.W2WTransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CurrenciesNotEqualValidator implements ConstraintValidator<CurrenciesNotEqual, W2WTransferRequest> {

    @Override
    public boolean isValid(W2WTransferRequest request, ConstraintValidatorContext context) {
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
