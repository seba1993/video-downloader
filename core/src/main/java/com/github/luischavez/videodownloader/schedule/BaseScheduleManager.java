package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.BaseManager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BaseScheduleManager extends BaseManager implements ScheduleManager {

    protected final Map<Long, ScheduleEntry> scheduleMap;

    public BaseScheduleManager(Context context) {
        super(context);

        this.scheduleMap = new ConcurrentHashMap<>();
    }

    @Override
    public void clear() {
        scheduleMap.clear();
    }

    @Override
    public ScheduleEntry get(long tag) {
        return scheduleMap.get(tag);
    }

    @Override
    public void add(long tag, Schedule schedule, ScheduleTask task) {
        scheduleMap.put(tag, new ScheduleEntry(schedule, task));
    }

    @Override
    public ScheduleEntry remove(long tag) {
        return scheduleMap.remove(tag);
    }

    protected Map<Schedule, ScheduleTask> getShouldRunningSchedules() {
        return scheduleMap.entrySet().stream()
                .map(entry -> entry.getValue())
                .filter(scheduleScheduleTaskEntry -> !(scheduleScheduleTaskEntry.getSchedule() instanceof NeverSchedule))
                .filter(scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getSchedule().calculateScheduleRange().isValid())
                .filter(scheduleScheduleTaskEntry -> (scheduleScheduleTaskEntry.getTask().getType().equals(ScheduleTask.Type.SINGLE_RUN) && !scheduleScheduleTaskEntry.getTask().wasRun())
                                                     || scheduleScheduleTaskEntry.getTask().getType().equals(ScheduleTask.Type.KEEP_RUNNING))
                //.filter(scheduleScheduleTaskEntry -> !scheduleScheduleTaskEntry.getValue().isRunning())
                .collect(Collectors.toMap(scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getSchedule(), scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getTask()));
    }

    protected Map<Schedule, ScheduleTask> getShouldNotRunningSchedules() {
        return scheduleMap.entrySet().stream()
                .map(entry -> entry.getValue())
                .filter(scheduleScheduleTaskEntry -> !scheduleScheduleTaskEntry.getSchedule().calculateScheduleRange().isValid())
                //.filter(scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getValue().isRunning())
                .collect(Collectors.toMap(scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getSchedule(), scheduleScheduleTaskEntry -> scheduleScheduleTaskEntry.getTask()));
    }

    protected void runScheduleTasks(Map<Schedule, ScheduleTask> scheduleTaskMap) {
        Iterator<Map.Entry<Schedule, ScheduleTask>> iterator = scheduleTaskMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Schedule, ScheduleTask> entry = iterator.next();

            final Schedule schedule = entry.getKey();
            final ScheduleTask task = entry.getValue();

            if (task.isRunning()) continue;

            getListeners(ScheduleListener.class).stream()
                    .forEach(scheduleListener -> scheduleListener.onScheduleTask(schedule, task));

            try {
                task.run();
            } catch (Exception ex) {
                getListeners(ScheduleListener.class).stream()
                        .forEach(scheduleListener -> scheduleListener.onScheduleException(schedule, task, ex));
            }
        }
    }

    protected void disableSchedules(Map<Schedule, ScheduleTask> scheduleTaskMap) {
        Iterator<Map.Entry<Schedule, ScheduleTask>> iterator = scheduleTaskMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Schedule, ScheduleTask> entry = iterator.next();

            final Schedule schedule = entry.getKey();
            final ScheduleTask task = entry.getValue();

            if (!task.isRunning()) continue;

            getListeners(ScheduleListener.class).stream()
                    .forEach(scheduleListener -> scheduleListener.onScheduleDisabled(schedule, task));

            try {
                task.disable();
            } catch (Exception ex) {
                getListeners(ScheduleListener.class).stream()
                        .forEach(scheduleListener -> scheduleListener.onScheduleException(schedule, task, ex));
            }
        }
    }
}
