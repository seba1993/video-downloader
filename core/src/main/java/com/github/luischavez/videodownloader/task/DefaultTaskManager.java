package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultTaskManager extends BaseTaskManager {

    @Inject
    public DefaultTaskManager(Context context) {
        super(context);
    }

    @Override
    protected boolean doWork() throws Exception {
        final ArrayList<Task> killed = new ArrayList<>();

        for (Task task : tasks.values()) {
            if (!task.isRunning()) {
                if (!task.isFresh()) {
                    killed.add(task);
                    continue;
                }
                try {
                    if (task.isFresh()) task.start();
                } catch (Exception ex) {
                    getListeners(TaskListener.class).stream()
                            .forEach(taskListener -> taskListener.onTaskException(task, ex));
                }
            }
        }

        if (!killed.isEmpty()) {
            List<Long> tags = tasks.entrySet().stream()
                    .filter(longTaskEntry -> killed.contains(longTaskEntry.getValue()))
                    .map(longTaskEntry -> longTaskEntry.getKey())
                    .collect(Collectors.toList());

            tags.stream().forEach(tasks::remove);
        }

        return true;
    }
}
