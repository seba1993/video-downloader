package com.github.luischavez.videodownloader.task;

public interface TaskStateListener {

    void onTaskStart(Task task);

    void onTaskStop(Task task);
}
