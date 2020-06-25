package com.github.luischavez.videodownloader.configuration;

import java.util.Objects;

public class BaseConfiguration implements Configuration {

    private final long uid;

    public BaseConfiguration(long uid) {
        this.uid = uid;
    }

    public BaseConfiguration() {
        this(System.nanoTime());
    }

    @Override
    public long uid() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof BaseConfiguration)) return false;
        BaseConfiguration that = (BaseConfiguration) o;
        return uid == that.uid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}
