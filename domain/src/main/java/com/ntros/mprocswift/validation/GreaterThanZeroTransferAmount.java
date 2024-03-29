package com.ntros.mprocswift.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = GreaterThanZeroTransferAmountValidator.class)
@Documented
public @interface GreaterThanZeroTransferAmount {

    String message() default "TransferAmount must be > 0";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}