package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LocalProcessTask extends ContextWrapper implements Task {

    private final AtomicBoolean starting;

    protected Process process;

    public LocalProcessTask(Context context) {
        super(context);

        starting = new AtomicBoolean(false);
    }

    protected abstract ProcessBuilder buildCommand() throws Exception;

    protected void stopProcess() {
        if (process == null || !process.isAlive()) return;
        process.descendants().forEach(processHandle -> processHandle.destroyForcibly());
        process.destroyForcibly();
    }

    @Override
    public long pid() {
        return process != null ? process.pid() : -1L;
    }

    @Override
    public boolean isFresh() {
        return !starting.get();
    }

    @Override
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    @Override
    public void start() throws Exception {
        if (isRunning()) return;

        starting.set(true);

        process = buildCommand()
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                //.redirectInput(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStart(this));
    }

    @Override
    public void kill() throws Exception {
        stopProcess();

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStop(this));
    }
}
