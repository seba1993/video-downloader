package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        final String value = object.toString();

        try {
            new URL(value);
        } catch (MalformedURLException ex) {
            return ValidationResult.fail(String.format("invalid url for field %s", prettyName));
        }

        return ValidationResult.pass();
    }
}
