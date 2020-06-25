package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.listener.Listener;
import com.github.luischavez.videodownloader.manager.Manager;

@Listener(TaskListener.class)
public interface TaskManager extends Manager {

    Task get(long tag);

    void add(long tag, Task task);

    void remove(long tag);
}
