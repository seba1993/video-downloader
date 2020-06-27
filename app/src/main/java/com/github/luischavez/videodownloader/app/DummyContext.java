package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.BaseContext;
import com.github.luischavez.videodownloader.system.DefaultSystem;
import com.github.luischavez.videodownloader.system.GuiceDependencyInjection;
import com.github.luischavez.videodownloader.system.System;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskListener;
import com.github.luischavez.videodownloader.task.TaskStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyContext extends BaseContext implements TaskListener, TaskStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyContext.class);

    private static DummyContext CONTEXT_INSTANCE;

    public DummyContext(System system) {
        super(system);
    }

    @Override
    public void log(Class<?> caller, String level, String message, Throwable cause) {
        if (cause != null) {
            LOGGER.error(message, cause);
        } else if (level.equals("error")) {
            LOGGER.error(message);
        }
    }

    public static DummyContext instance() {
        if (CONTEXT_INSTANCE == null) CONTEXT_INSTANCE = new DummyContext(new DefaultSystem(new GuiceDependencyInjection()));

        return CONTEXT_INSTANCE;
    }

    @Override
    public void onTaskAdded(Task task) {

    }

    @Override
    public void onTaskRemoved(Task task) {

    }

    @Override
    public void onTaskException(Task task, Throwable throwable) {
        error(DummyContext.class, "task exception", throwable);
    }

    @Override
    public void onTaskStart(Task task) {

    }

    @Override
    public void onTaskStop(Task task) {

    }
}
