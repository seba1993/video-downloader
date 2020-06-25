package com.github.luischavez.videodownloader.support;

public class MediaNotFoundException extends RuntimeException {

    public MediaNotFoundException() {
    }

    public MediaNotFoundException(String message) {
        super(message);
    }

    public MediaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaNotFoundException(Throwable cause) {
        super(cause);
    }

    public MediaNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
