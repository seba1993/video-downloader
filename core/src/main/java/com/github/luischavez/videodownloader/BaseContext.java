package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.system.System;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class BaseContext implements Context {

    private final System system;

    public BaseContext(System system) {
        this.system = system;
    }

    public System getSystem() {
        return system;
    }

    public static String resolveWorkingDir() {
        try {
            URI location = BaseContext.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File file = new File(location);

            if (file.isFile()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    return parent.getAbsolutePath();
                }
            }

            if (file.exists()) {
                return file.getAbsolutePath();
            }
        } catch (Exception ex) {
            // Fallback below.
        }

        return java.lang.System.getProperty("user.dir");
    }

    public String getWorkingDir() {
        return resolveWorkingDir();
    }

    @Override
    public String getFileSeparator() {
        return java.lang.System.getProperty("file.separator");
    }

    @Override
    public String buildPath(String... elements) {
        return Arrays.asList(elements).stream().collect(Collectors.joining(getFileSeparator()));
    }
}
