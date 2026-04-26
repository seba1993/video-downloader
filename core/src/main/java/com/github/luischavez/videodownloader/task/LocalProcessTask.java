package com.github.luischavez.videodownloader.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class LocalProcessTask extends ContextWrapper implements Task {

    private final AtomicBoolean starting;

    protected Process process;

    protected Consumer<String> debugger;

    public LocalProcessTask(Context context) {
        super(context);

        starting = new AtomicBoolean(false);
        debugger = null;
    }

    public void setDebugger(Consumer<String> debugger) {
        this.debugger = debugger;
    }

    protected abstract ProcessBuilder buildCommand() throws Exception;

    protected long getStopTimeoutMillis() {
        return 0L;
    }

    protected void stopProcess() {
        if (process == null || !process.isAlive()) return;
        long stopTimeoutMillis = getStopTimeoutMillis();

        if (stopTimeoutMillis > 0L) {
            process.destroy();

            try {
                if (process.waitFor(stopTimeoutMillis, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

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

        final ProcessBuilder command = buildCommand();

        if (debugger != null) {
            process = command
                    .redirectErrorStream(true)
                    .start();

            new DebugThread().start();
        } else {
            process = command
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    //.redirectInput(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStart(this));
    }

    @Override
    public void kill() throws Exception {
        stopProcess();

        getListeners(TaskStateListener.class).stream()
                .forEach(taskStateListener -> taskStateListener.onTaskStop(this));
    }

    private class DebugThread extends Thread implements Runnable {

        public DebugThread() {
            setName(LocalProcessTask.class.getSimpleName());
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            BufferedReader reader = null;

            try {
                inputStream = process.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (Exception ex) {
                debugger.accept(ex.getMessage());
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

                            debugger.accept(line);
                        }
                    }
                } catch (Exception ex) {
                    debugger.accept(ex.getMessage());
                    break;
                }
            }
        }
    }
}
