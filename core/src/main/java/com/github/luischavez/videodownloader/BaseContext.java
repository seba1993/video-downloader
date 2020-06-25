package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.system.System;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class BaseContext implements Context {

    private final System system;

    @Injected
    public BaseContext(System system) {
        this.system = system;
    }

    public System getSystem() {
        return system;
    }

    public String getWorkingDir() {
        return java.lang.System.getProperty("user.dir");
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
