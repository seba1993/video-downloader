package com.github.luischavez.videodownloader.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.listener.Listenable;
import com.github.luischavez.videodownloader.listener.Listener;

@Listener(ManagerListener.class)
public interface Manager extends Context, Listenable {

    boolean isRunning();
    void start() throws ManagerStateException;
    void stop() throws ManagerStateException;
}
