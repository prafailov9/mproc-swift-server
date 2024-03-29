package com.ntros.mprocswift.validation;

import com.ntros.mprocswift.dto.RangeRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OriginLessThanBoundValidator implements ConstraintValidator<OriginLessThanBound, RangeRequest> {


    @Override
    public boolean isValid(RangeRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // not validating null here
        }
        return value.getOrigin() <= value.getBound();
    }
}
