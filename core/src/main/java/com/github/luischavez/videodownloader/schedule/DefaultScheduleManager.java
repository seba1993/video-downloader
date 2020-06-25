package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.system.Injected;

import java.util.Map;

public class DefaultScheduleManager extends BaseScheduleManager {

    @Injected
    public DefaultScheduleManager(Context context) {
        super(context);
    }

    @Override
    protected boolean doWork() throws Exception {
        Map<Schedule, ScheduleTask> shouldRunningSchedules = getShouldRunningSchedules();
        Map<Schedule, ScheduleTask> shouldNotRunningSchedules = getShouldNotRunningSchedules();

        runScheduleTasks(shouldRunningSchedules);
        disableSchedules(shouldNotRunningSchedules);

        return true;
    }
}
