package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class StreamTime {

    private StreamTime() {
    }

    public static ZoneId zone(StreamConfiguration configuration) {
        if (configuration == null) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(configuration.getTimeZoneId());
        } catch (DateTimeException ex) {
            return ZoneId.systemDefault();
        }
    }

    public static ZonedDateTime now(StreamConfiguration configuration) {
        return ZonedDateTime.now(zone(configuration));
    }

    public static ZonedDateTime nextDailySplit(StreamConfiguration configuration, Instant now) {
        ZoneId zone = zone(configuration);
        ZonedDateTime localNow = now.atZone(zone);
        LocalDate date = localNow.toLocalDate();
        LocalTime splitAt = configuration == null
                ? LocalTime.MIDNIGHT
                : configuration.getDailySplitAt();
        ZonedDateTime boundary = ZonedDateTime.of(date, splitAt, zone);

        if (!boundary.isAfter(localNow)) {
            boundary = boundary.plusDays(1);
        }

        return boundary;
    }

    public static long millisUntilNextDailySplit(StreamConfiguration configuration, Instant now) {
        return Math.max(1L, Duration.between(now, nextDailySplit(configuration, now).toInstant()).toMillis());
    }
}
