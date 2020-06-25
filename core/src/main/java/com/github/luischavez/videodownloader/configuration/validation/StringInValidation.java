package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

import java.util.Arrays;

public class StringInValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        String value = object.toString();

        if (!Arrays.asList(params).contains(value)) {
            return ValidationResult.fail(String.format("invalid value %s for field %s", value, prettyName));
        }

        return ValidationResult.pass();
    }
}
