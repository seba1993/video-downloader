package com.github.luischavez.videodownloader.schedule;

public interface ScheduleListener {

    void onScheduleTask(Schedule schedule, ScheduleTask task);
    void onScheduleDisabled(Schedule schedule, ScheduleTask task);
    void onScheduleException(Schedule schedule, ScheduleTask task, Throwable throwable);
}
