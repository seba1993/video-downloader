package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;

public abstract class SingleScheduleTask extends BaseScheduleTask {

    public SingleScheduleTask(Context context) {
        super(context);
    }

    @Override
    public Type getType() {
        return Type.SINGLE_RUN;
    }
}
