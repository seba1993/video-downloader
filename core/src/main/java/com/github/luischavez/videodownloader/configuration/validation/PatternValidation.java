package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

public class PatternValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        String pattern = params[0];
        String message = params[1];

        String value = object.toString();

        if (!value.matches(pattern)) {
            return ValidationResult.fail(String.format(message, prettyName));
        }

        return ValidationResult.pass();
    }
}
