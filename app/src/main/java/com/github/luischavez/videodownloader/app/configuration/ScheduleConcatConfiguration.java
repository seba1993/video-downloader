package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.NotEmptyValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validable;
import com.github.luischavez.videodownloader.schedule.Schedule;

public class ScheduleConcatConfiguration extends BaseConfiguration {

    @Validable(value = NotEmptyValidation.class, name = "Schedule")
    private Schedule schedule;

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }
}
