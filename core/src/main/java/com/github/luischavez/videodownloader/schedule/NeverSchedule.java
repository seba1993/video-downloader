package com.github.luischavez.videodownloader.schedule;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;

public class NeverSchedule extends Schedule {

    public NeverSchedule() {
        super(Day.Everyday, null, 0);
    }

    @Override
    public ScheduleRange calculateScheduleRange() {
        return new NeverScheduleRange();
    }

    public static class NeverScheduleRange extends ScheduleRange {

        public NeverScheduleRange() {
            super(null, null);
        }

        @Override
        public LocalDateTime getStartAt() {
            return LocalDateTime.now();
        }

        @Override
        public LocalDateTime getStopAt() {
            return null;
        }

        @Override
        public long timeToStart(TemporalUnit temporalUnit) {
            return 1;
        }

        @Override
        public long timeToStop(TemporalUnit temporalUnit) {
            return 1;
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }
}
