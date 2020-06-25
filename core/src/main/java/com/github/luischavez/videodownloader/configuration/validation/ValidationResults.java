package com.github.luischavez.videodownloader.configuration.validation;

import java.util.ArrayList;

public class ValidationResults extends ArrayList<ValidationResult> {

    public boolean pass() {
        return stream().filter(validationResult -> !validationResult.isSuccess()).count() == 0;
    }

    public boolean fails() {
        return !pass();
    }
}
