package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ThreadProcessTask extends ContextWrapper implements Task {

    private final AtomicBoolean starting;

    protected TaskThread taskThread;

    protected Process process;
    protected InputStream inputStream;

    private long interval;

    public ThreadProcessTask(Context context) {
        super(context);

        starting = new AtomicBoolean(false);
        interval = 50L;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    protected abstract ProcessBuilder buildCommand() throws Exception;

    protected abstract void onInput(String line);

    protected abstract void onStart();
    protected abstract void onStop();

    protected void stopProcess() {
        if (process == null || !process.isAlive()) return;
        process.descendants().forEach(processHandle -> processHandle.destroyForcibly());
        process.destroyForcibly();
    }

    @Override
    public long pid() {
        return process != null ? process.pid() : -1;
    }

    @Override
    public boolean isFresh() {
        return !starting.get();
    }

    @Override
    public boolean isRunning() {
        return (taskThread != null && taskThread.isAlive()) || (process != null && process.isAlive());
    }

    @Override
    public void start() throws Exception {
        if (isRunning()) return;

        starting.set(true);

        process = buildCommand()
                .redirectErrorStream(true)
                .start();

        taskThread = new TaskThread();
        taskThread.start();

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStart(this));
    }

    @Override
    public void kill() throws Exception {
        stopProcess();

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStop(this));
    }

    private class TaskThread extends Thread implements Runnable {

        public TaskThread() {
            setName(TaskThread.class.getSimpleName());
        }

        @Override
        public void run() {
            onStart();
            BufferedReader reader = null;
            try {
                inputStream = process.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (Exception ex) {
                onInput(ex.getMessage());
                stopProcess();
            }

            while (process.isAlive()) {
                try {
                    if(inputStream.available() > 0) {
                        int data = reader.read();

                        if (data != -1) {
                            StringBuilder buffer = new StringBuilder();

                            while (data != -1 && data != (int) '\n') {
                                buffer.append((char) data);
                                data = reader.read();
                            }

                            String line = buffer.toString();
                            onInput(line);
                        }
                    }

                    Thread.sleep(interval);
                } catch (Exception ex) {
                    onInput(ex.getMessage());
                    break;
                }
            }

            try {
                kill();
            } catch (Exception ex) {
                onInput(ex.getMessage());
            }

            onStop();
        }
    }
}
