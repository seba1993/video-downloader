package com.github.luischavez.videodownloader.schedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulePicker extends Schedule {

    private final List<Schedule> schedules;

    public SchedulePicker(Schedule... schedules) {
        super(null, null, 0);
        this.schedules = new ArrayList<>(Arrays.asList(schedules));
    }

    public SchedulePicker(List<Schedule> schedules) {
        super(null, null, 0);
        this.schedules = new ArrayList<>(schedules);
    }

    private Schedule getSchedule() {
        schedules.sort((schedule1, schedule2) -> {
            ScheduleRange scheduleRange1 = schedule1.calculateScheduleRange();
            ScheduleRange scheduleRange2 = schedule2.calculateScheduleRange();

            return scheduleRange1.getStartAt().compareTo(scheduleRange2.getStartAt());
        });

        return schedules.get(0);
    }

    @Override
    public Day getDay() {
        return getSchedule().getDay();
    }

    @Override
    public LocalTime getStartAtTime() {
        return getSchedule().getStartAtTime();
    }

    @Override
    public long getDuration() {
        return getSchedule().getDuration();
    }
}
