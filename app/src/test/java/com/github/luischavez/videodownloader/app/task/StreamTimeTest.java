package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class StreamTimeTest {

    public static void main(String[] args) {
        testConfigurationDefaults();
        testMidnightInConfiguredZone();
        testDaylightSavingTransition();
        testMexicoCurrentRules();
        testBrazilCurrentRules();
        java.lang.System.out.println("StreamTimeTest passed");
    }

    private static void testConfigurationDefaults() {
        StreamConfiguration configuration = new StreamConfiguration();

        assertEquals(ZoneId.systemDefault().getId(), configuration.getTimeZoneId(), "default time zone");
        assertEquals(LocalTime.MIDNIGHT, configuration.getDailySplitAt(), "default split time");
        assertEquals(false, configuration.isDailySplit(), "daily split default");
    }

    private static void testMidnightInConfiguredZone() {
        StreamConfiguration configuration = configuration("Asia/Jerusalem", LocalTime.MIDNIGHT);
        Instant now = Instant.parse("2026-09-28T12:00:00Z");
        ZonedDateTime next = StreamTime.nextDailySplit(configuration, now);

        assertEquals(LocalDate.of(2026, 9, 29), next.toLocalDate(), "Jerusalem split date");
        assertEquals(LocalTime.MIDNIGHT, next.toLocalTime(), "Jerusalem split time");
        assertEquals(ZoneId.of("Asia/Jerusalem"), next.getZone(), "Jerusalem split zone");
    }

    private static void testDaylightSavingTransition() {
        StreamConfiguration configuration = configuration("America/New_York", LocalTime.MIDNIGHT);
        Instant afterMidnightBeforeDst = Instant.parse("2026-03-08T05:01:00Z");
        ZonedDateTime next = StreamTime.nextDailySplit(configuration, afterMidnightBeforeDst);

        assertEquals(LocalDate.of(2026, 3, 9), next.toLocalDate(), "DST split date");
        assertEquals(LocalTime.MIDNIGHT, next.toLocalTime(), "DST split time");
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"), next.toInstant(), "DST-aware instant");
    }

    private static void testMexicoCurrentRules() {
        ZoneOffset offset = ZoneId.of("America/Mexico_City")
                .getRules()
                .getOffset(Instant.parse("2026-07-01T18:00:00Z"));
        assertEquals(ZoneOffset.ofHours(-6), offset, "Mexico City 2026 offset");
    }

    private static void testBrazilCurrentRules() {
        ZoneOffset offset = ZoneId.of("America/Sao_Paulo")
                .getRules()
                .getOffset(Instant.parse("2026-01-15T15:00:00Z"));
        assertEquals(ZoneOffset.ofHours(-3), offset, "Sao Paulo 2026 offset");
    }

    private static StreamConfiguration configuration(String zoneId, LocalTime splitAt) {
        StreamConfiguration configuration = new StreamConfiguration();
        configuration.setTimeZoneId(zoneId);
        configuration.setDailySplit(true);
        configuration.setDailySplitAt(splitAt);
        return configuration;
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
}
