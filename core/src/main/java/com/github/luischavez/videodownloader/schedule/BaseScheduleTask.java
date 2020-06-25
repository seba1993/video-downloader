package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseScheduleTask extends ContextWrapper implements ScheduleTask {

    private final AtomicBoolean ran;
    private final AtomicLong retryCount;
    private final AtomicBoolean disabling;

    public BaseScheduleTask(Context context) {
        super(context);

        ran = new AtomicBoolean(false);
        retryCount = new AtomicLong(0);
        disabling = new AtomicBoolean(false);
    }

    protected abstract void doTask() throws Exception;
    protected abstract void doDisable() throws Exception;

    @Override
    public boolean wasRun() {
        return ran.get();
    }

    @Override
    public long getRetryCount() {
        return retryCount.get();
    }

    @Override
    public void disable() throws Exception {
        disabling.set(true);
        doDisable();
        disabling.set(false);
    }

    @Override
    public void run() throws Exception {
        if (disabling.get()) return;
        if (getType().equals(Type.SINGLE_RUN) && ran.get()) return;
        if(ran.get()) retryCount.incrementAndGet();

        ran.set(true);

        doTask();
    }
}
