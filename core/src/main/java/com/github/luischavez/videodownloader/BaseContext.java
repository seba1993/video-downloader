package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.system.System;

public abstract class BaseContext implements Context {

    private final System system;

    @Injected
    public BaseContext(System system) {
        this.system = system;
    }

    public String getWorkingDir() {
        return java.lang.System.getProperty("user.dir");
    }

    public System getSystem() {
        return system;
    }

    @Override
    public void log(Class<?> caller, String level, String message, Throwable cause) {
        java.lang.System.out.println(message);
    }
}
