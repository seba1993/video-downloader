package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.BaseManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseTaskManager extends BaseManager implements TaskManager {

    protected final Map<Long, Task> tasks;

    public BaseTaskManager(Context context) {
        super(context);

        tasks = new ConcurrentHashMap<>();
    }

    @Override
    public Task get(long tag) {
        return tasks.get(tag);
    }

    @Override
    public void add(long tag, Task task) {
        tasks.put(tag, task);

        try {
            if (!task.isRunning()) task.start();
        } catch (Exception ex) {
            getListeners(TaskListener.class).stream()
                    .forEach(taskListener -> taskListener.onTaskException(task, ex));
        }

        getListeners(TaskListener.class).stream()
                .forEach(taskListener -> taskListener.onTaskAdded(task));
    }

    @Override
    public void remove(long tag) {
        Task task = get(tag);

        try {
            if (task.isRunning()) task.kill();
        } catch (Exception ex) {
            getListeners(TaskListener.class).stream()
                    .forEach(taskListener -> taskListener.onTaskException(task, ex));
        }

        tasks.remove(tag);

        getListeners(TaskListener.class).stream()
                .forEach(taskListener -> taskListener.onTaskRemoved(task));
    }
}
