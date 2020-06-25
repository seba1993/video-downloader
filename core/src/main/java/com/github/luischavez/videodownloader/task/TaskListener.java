package com.github.luischavez.videodownloader.task;

public interface TaskListener {

    void onTaskAdded(Task task);

    void onTaskRemoved(Task task);

    void onTaskException(Task task, Throwable throwable);
}
