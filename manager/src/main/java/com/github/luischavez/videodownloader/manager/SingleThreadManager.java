package com.github.luischavez.videodownloader.manager;

import com.github.luischavez.videodownloader.Context;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SingleThreadManager extends BaseManager {

    private final ManagerThread managerThread;

    private long executionInterval = 1_000;

    public SingleThreadManager(Context context) {
        super(context);

        managerThread = new ManagerThread();
    }

    protected abstract boolean doWork() throws Exception;

    public long getExecutionInterval() {
        return executionInterval;
    }

    public void setExecutionInterval(long executionInterval) {
        this.executionInterval = executionInterval;
    }

    public boolean isRunning() {
        return managerThread.isRunning();
    }

    public void start() throws ManagerStateException {
        try {
            managerThread.start();
        } catch (IllegalThreadStateException ex) {
            throw new ManagerStateException("can't start thread", ex);
        }
    }

    public void stop() throws ManagerStateException {
        if (!managerThread.isRunning()) throw new ManagerStateException("manager not running");

        managerThread.stopExecution();
    }

    private class ManagerThread extends Thread implements Runnable {

        private final AtomicBoolean running;

        private ManagerThread() {
            running = new AtomicBoolean(false);
        }

        private void handleInit() {
            running.set(true);

            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerStart(SingleThreadManager.this));
        }

        private void handleFinish() {
            running.set(false);

            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerStop(SingleThreadManager.this));
        }

        private void handleException(String message, Throwable cause) {
            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerExecutionException(SingleThreadManager.this, message, cause));
        }

        public boolean isRunning() {
            return running.get();
        }

        public void stopExecution() {
            running.set(false);
        }

        @Override
        public void run() {
            handleInit();

            while (isRunning()) {
                try {
                    if (!doWork()) {
                        stopExecution();
                    }
                } catch (Exception ex) {
                    handleException("exception in thread execution", ex);
                }

                try {
                    Thread.sleep(getExecutionInterval());
                } catch (InterruptedException ex) {
                    handleException("can't sleep thread, interval: " + getExecutionInterval(), ex);
                }
            }

            handleFinish();
        }
    }
}
