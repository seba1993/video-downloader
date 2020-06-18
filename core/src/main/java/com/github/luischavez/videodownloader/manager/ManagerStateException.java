package com.github.luischavez.videodownloader.manager;

public class ManagerStateException extends RuntimeException {

    public ManagerStateException() {
    }

    public ManagerStateException(String message) {
        super(message);
    }

    public ManagerStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagerStateException(Throwable cause) {
        super(cause);
    }

    public ManagerStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
