package com.github.luischavez.videodownloader.util;

import java.io.*;
import java.util.stream.Collectors;

public final class FileUtils {

    public static String inputStreamAsString(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            // IGNORE
        }

        return null;
    }

    public static String resourceAsString(Class<?> classLoader, String resourceName) {
        return inputStreamAsString(classLoader.getResourceAsStream(resourceName));
    }

    public static String fileAsString(String fileName, String directory) {
        directory = directory == null ? System.getProperty("user.dir") : directory;
        String fileSeparator = System.getProperty("file.separator");
        String filePath = directory + fileSeparator + fileName;

        try {
            return inputStreamAsString(new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException ex) {
            // IGNORE
        }

        return null;
    }
}
