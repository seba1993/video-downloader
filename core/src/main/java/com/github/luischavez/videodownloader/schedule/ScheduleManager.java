package com.github.luischavez.videodownloader.schedule;

import com.github.luischavez.videodownloader.listener.Listener;
import com.github.luischavez.videodownloader.manager.Manager;

@Listener(ScheduleListener.class)
public interface ScheduleManager extends Manager {

    void clear();

    ScheduleEntry get(long tag);

    void add(long tag, Schedule schedule, ScheduleTask task);

    ScheduleEntry remove(long tag);

    class ScheduleEntry {

        private final Schedule schedule;
        private final ScheduleTask task;

        public ScheduleEntry(Schedule schedule, ScheduleTask task) {
            this.schedule = schedule;
            this.task = task;
        }

        public Schedule getSchedule() {
            return schedule;
        }

        public ScheduleTask getTask() {
            return task;
        }
    }
}
