package com.github.luischavez.videodownloader.configuration.validation;

public class ValidationResult {

    private final boolean success;
    private final String message;

    protected ValidationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public static ValidationResult pass() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }
}
