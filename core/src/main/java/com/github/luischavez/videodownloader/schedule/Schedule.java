package com.github.luischavez.videodownloader.schedule;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class Schedule implements Serializable {

    static final long serialVersionUID = 1L;

    public static final long ONE_MILLISECOND = 1L;
    public static final long ONE_SECOND = ONE_MILLISECOND * 1000;
    public static final long ONE_MINUTE = ONE_SECOND * 60;
    public static final long ONE_HOUR = ONE_MINUTE * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;

    private Day day;
    private LocalTime startAtTime;
    private long duration;

    public Schedule(Day day, LocalTime startAtTime, long duration) {
        this.day = day;
        this.startAtTime = startAtTime;
        this.duration = duration;
    }

    public Day getDay() {
        return day;
    }

    public void setDay(Day day) {
        this.day = day;
    }

    public LocalTime getStartAtTime() {
        return startAtTime;
    }

    public void setStartAtTime(LocalTime startAtTime) {
        this.startAtTime = startAtTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public ScheduleRange calculateScheduleRange() {
        final Day day = getDay();
        final LocalTime startAtTime = getStartAtTime();
        final long duration = getDuration();

        final LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = LocalDateTime.of(now.toLocalDate(), startAtTime);

        final int currentDayIndex = now.getDayOfWeek().getValue();
        final int dayIndex = day.value == 0 ? currentDayIndex : day.value;

        startAt = startAt.with(ChronoField.DAY_OF_WEEK, dayIndex);
        startAt = startAt.withSecond(0);

        LocalDateTime stopAt = startAt.plus(duration, ChronoUnit.MILLIS);

        if (startAt.isBefore(now) && stopAt.isBefore(now)) {
            startAt = startAt.plusWeeks(1);
            stopAt = stopAt.plusWeeks(1);
        }

        return new ScheduleRange(startAt, stopAt);
    }

    public enum Day implements Serializable {

        Everyday(0), Monday(1), Tuesday(2), Wednesday(3), Thursday(4), Friday(5), Saturday(6), Sunday(7);

        int value;

        Day(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class ScheduleRange implements Serializable {

        static final long serialVersionUID = 1L;

        private final LocalDateTime startAt;
        private final LocalDateTime stopAt;

        public ScheduleRange(LocalDateTime startAt, LocalDateTime stopAt) {
            this.startAt = startAt;
            this.stopAt = stopAt;
        }

        public LocalDateTime getStartAt() {
            return startAt;
        }

        public LocalDateTime getStopAt() {
            return stopAt;
        }

        public long timeToStart(TemporalUnit temporalUnit) {
            final LocalDateTime now = LocalDateTime.now();

            return now.until(startAt, temporalUnit);
        }

        public long timeToStop(TemporalUnit temporalUnit) {
            final LocalDateTime now = LocalDateTime.now();

            return now.until(stopAt, temporalUnit);
        }

        public boolean isValid() {
            return timeToStart(ChronoUnit.SECONDS) <= 0 && timeToStop(ChronoUnit.SECONDS) >= 0;
        }
    }
}
