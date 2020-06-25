package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

public class NotEmptyValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        boolean empty = isEmpty(object);

        if (params.length > 1) {
            final String otherField = params[0];
            final String desireValue = params[1];
            final Object otherValue = getFieldValue(configuration, otherField);

            empty &= desireValue.equals(otherValue.toString());
        }

        return empty ? ValidationResult.fail(String.format("%s must not be empty", prettyName)) : ValidationResult.pass();
    }
}
