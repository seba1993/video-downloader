package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;

public abstract class KeepRunningScheduleTask extends BaseScheduleTask {

    public KeepRunningScheduleTask(Context context) {
        super(context);
    }

    @Override
    public Type getType() {
        return Type.KEEP_RUNNING;
    }
}
