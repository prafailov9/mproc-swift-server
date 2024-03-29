package com.ntros.mprocswift.validation;

import com.ntros.mprocswift.dto.transfer.InternalTransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;

public class AccountNumbersNotEqualValidator implements ConstraintValidator<AccountNumbersNotEqual, InternalTransferRequest> {
    @Override
    public boolean isValid(InternalTransferRequest internalTransferRequest, ConstraintValidatorContext context) {
        return baseValidation(internalTransferRequest)
                || !internalTransferRequest.getSourceAccountNumber().equals(internalTransferRequest.getRecipientAccountNumber());

    }

    private boolean baseValidation(InternalTransferRequest internalTransferRequest) {
        return internalTransferRequest == null || Strings.isEmpty(internalTransferRequest.getSourceAccountNumber())
                || Strings.isEmpty(internalTransferRequest.getRecipientAccountNumber());
    }
}
