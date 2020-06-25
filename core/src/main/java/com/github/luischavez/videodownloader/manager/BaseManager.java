package com.github.luischavez.videodownloader.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseManager extends ContextWrapper implements Manager {

    private ManagerThread managerThread;

    private long executionInterval = 1_000;

    public BaseManager(Context context) {
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
            if (managerThread.isRunning()) {
                managerThread.stopExecution();
            }

            managerThread = new ManagerThread();
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

            setName(BaseManager.this.getClass().getName() + "-Thread");
        }

        private void handleInit() {
            running.set(true);

            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerStart(BaseManager.this));
        }

        private void handleFinish() {
            running.set(false);

            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerStop(BaseManager.this));
        }

        private void handleException(String message, Throwable cause) {
            getListeners(ManagerListener.class).stream()
                    .forEach(managerListener -> managerListener.onManagerExecutionException(BaseManager.this, message, cause));
        }

        public boolean isRunning() {
            return running.get() && isAlive();
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
