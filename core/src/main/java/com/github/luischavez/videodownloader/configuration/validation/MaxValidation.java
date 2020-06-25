package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

import java.math.BigDecimal;

public class MaxValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        final BigDecimal decimal = new BigDecimal(object.toString());
        final BigDecimal max = new BigDecimal(params[0]);

        if (decimal.doubleValue() > max.doubleValue()) {
            return ValidationResult.fail(String.format("the maximum value for the field %s is %s", prettyName, max));
        }

        return ValidationResult.pass();
    }
}
