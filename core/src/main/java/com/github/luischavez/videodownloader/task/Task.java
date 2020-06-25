package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.listener.Listener;

@Listener(TaskStateListener.class)
public interface Task extends Context {

    long pid();

    String details();

    boolean isFresh();

    boolean isRunning();

    void start() throws Exception;

    void kill() throws Exception;
}
