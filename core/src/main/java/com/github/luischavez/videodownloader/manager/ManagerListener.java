package com.github.luischavez.videodownloader.manager;

public interface ManagerListener {

    void onManagerStart(Manager manager);
    void onManagerStop(Manager manager);
    void onManagerExecutionException(Manager manager, String message, Throwable throwable);
}
