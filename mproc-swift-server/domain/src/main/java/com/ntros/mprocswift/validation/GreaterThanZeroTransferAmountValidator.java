package com.ntros.mprocswift.validation;

import com.ntros.mprocswift.dto.transfer.TransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class GreaterThanZeroTransferAmountValidator implements ConstraintValidator<GreaterThanZeroTransferAmount, TransferRequest> {
    @Override
    public boolean isValid(TransferRequest transferRequest, ConstraintValidatorContext context) {
        if (transferRequest == null) {
            return true;
        } else if (transferRequest.getAmount() == null) {
            return true;
        }
        log.info("IN AMOUNT VALIDATOR");
        return transferRequest.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
