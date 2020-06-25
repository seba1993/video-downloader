package com.github.luischavez.videodownloader.support;

public class MediaOfflineException extends RuntimeException {

    public MediaOfflineException() {
    }

    public MediaOfflineException(String message) {
        super(message);
    }

    public MediaOfflineException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaOfflineException(Throwable cause) {
        super(cause);
    }

    public MediaOfflineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
