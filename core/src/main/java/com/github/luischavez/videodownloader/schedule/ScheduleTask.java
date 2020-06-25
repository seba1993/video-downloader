package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;

public interface ScheduleTask extends Context {

    Type getType();

    boolean wasRun();

    long getRetryCount();

    boolean isRunning();

    void disable() throws Exception;

    void run() throws Exception;

    enum Type {

        KEEP_RUNNING, SINGLE_RUN
    }
}
