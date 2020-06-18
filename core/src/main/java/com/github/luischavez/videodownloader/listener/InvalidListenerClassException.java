package com.github.luischavez.videodownloader.listener;

public class InvalidListenerClassException extends RuntimeException {

    public InvalidListenerClassException() {
    }

    public InvalidListenerClassException(String message) {
        super(message);
    }

    public InvalidListenerClassException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidListenerClassException(Throwable cause) {
        super(cause);
    }

    public InvalidListenerClassException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
