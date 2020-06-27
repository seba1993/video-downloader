package com.github.luischavez.videodownloader.app.util;

public final class FormatUtils {

    public static String formatTime(long millis) {
        long seconds = millis / 1_000;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;

        return String.format("%s:%s:%s",
                hours < 10 ? "0" + hours : hours,
                minutes < 10 ? "0" + minutes : minutes,
                seconds < 10 ? "0" + seconds : seconds);
    }
}
