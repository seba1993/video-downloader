package com.github.luischavez.videodownloader.util;

public final class PlatformUtils {

    public static boolean isWindowsHost() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean kill(long pid) {
        ProcessHandle processHandle = null;

        try {
            processHandle = ProcessHandle.of(pid).get();
        } catch (Exception ex) {
            // IGNORE
        }

        if (processHandle != null) {
            processHandle.descendants().forEach(decendant -> decendant.destroyForcibly());

            return processHandle.destroyForcibly();
        }

        return false;
    }

    public static boolean isRunning(long pid) {
        ProcessHandle processHandle;

        try {
            processHandle = ProcessHandle.of(pid).get();
        } catch (Exception ex) {
            return false;
        }

        return processHandle.isAlive();
    }
}
