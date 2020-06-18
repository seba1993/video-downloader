package com.github.luischavez.videodownloader;

public interface Logger {

    void log(Class<?> caller, String level, String message, Throwable cause);

    default void log(Class<?> caller, String level, String message) {
        log(caller, level, message, null);
    }

    default void info(Class<?> caller, String message) {
        log(caller, "info", message, null);
    }

    default void debug(Class<?> caller, String message) {
        log(caller, "debug", message, null);
    }

    default void error(Class<?> caller, String message, Throwable cause) {
        log(caller, "error", message, cause);
    }

    default void error(Class<?> caller, String message) {
        error(caller, message, null);
    }
}
