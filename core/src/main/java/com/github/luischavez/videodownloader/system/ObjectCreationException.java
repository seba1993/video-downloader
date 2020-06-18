package com.github.luischavez.videodownloader.system;

public class ObjectCreationException extends RuntimeException {

    public ObjectCreationException() {
    }

    public ObjectCreationException(String message) {
        super(message);
    }

    public ObjectCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectCreationException(Throwable cause) {
        super(cause);
    }

    public ObjectCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
