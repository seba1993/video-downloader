package com.github.luischavez.videodownloader.app.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.time.LocalTime;
import java.time.ZoneId;

public class StreamConfigurationCompatibilityTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: StreamConfigurationCompatibilityTest CONFIGURATION_DIRECTORY");
        }

        File[] files = new File(args[0]).listFiles((directory, name) -> name.endsWith(".bin"));
        if (files == null) {
            throw new IllegalArgumentException("cannot read configuration directory " + args[0]);
        }

        int streamCount = 0;
        for (File file : files) {
            try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
                Object value = input.readObject();
                if (!(value instanceof StreamConfiguration)) {
                    continue;
                }

                StreamConfiguration configuration = (StreamConfiguration) value;
                assertEquals(ZoneId.systemDefault().getId(), configuration.getTimeZoneId(), file.getName());
                assertEquals(LocalTime.MIDNIGHT, configuration.getDailySplitAt(), file.getName());
                assertEquals(false, configuration.isDailySplit(), file.getName());
                streamCount++;
            }
        }

        if (streamCount == 0) {
            throw new AssertionError("no stream configurations found");
        }

        java.lang.System.out.println("Loaded " + streamCount + " legacy stream configurations");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
}
