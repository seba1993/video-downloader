package com.github.luischavez.videodownloader.streamclient.api;

import com.github.luischavez.videodownloader.streamclient.api.gui.model.Subtitle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String SUBTITLE_REGEX = "(?<index>\\d+)\\n(?<start>[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) --> (?<end>[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3})\\n(?<text>.+)";

    public static Date parseDate(String time) {
        Pattern pattern = Pattern.compile("(?<h>[0-9]{2}):(?<m>[0-9]{2}):(?<s>[0-9]{2}),(?<ms>[0-9]{3})");
        Matcher matcher = pattern.matcher(time);

        if (matcher.find()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(0, 0, 0);
            calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(matcher.group("h")));
            calendar.set(Calendar.MINUTE, Integer.valueOf(matcher.group("m")));
            calendar.set(Calendar.SECOND, Integer.valueOf(matcher.group("s")));
            calendar.set(Calendar.MILLISECOND, Integer.valueOf(matcher.group("ms")));

            return calendar.getTime();
        }

        return null;
    }

    public static String parseDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String hours = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
        String seconds = String.valueOf(calendar.get(Calendar.SECOND));
        String millis = String.valueOf(calendar.get(Calendar.MILLISECOND));

        hours = hours.length() < 2 ? "0" + hours : hours;
        minutes = minutes.length() < 2 ? "0" + minutes : minutes;
        seconds = seconds.length() < 2 ? "0" + seconds : seconds;
        while (millis.length() < 3) millis = "0" + millis;

        return String.format("%s:%s:%s,%s", hours, minutes, seconds, millis);
    }

    public static List<Subtitle> parseSubtitle(String subtitle) {
        Matcher matcher = Pattern.compile(SUBTITLE_REGEX, Pattern.MULTILINE).matcher(subtitle);

        ArrayList<Subtitle> subtitles = new ArrayList<>();
        while (matcher.find()) {
            String start = matcher.group("start");
            String end = matcher.group("end");
            String text = matcher.group("text");

            subtitles.add(new Subtitle(parseDate(start), parseDate(end), text));
        }

        return subtitles;
    }

    public static String parseSubtitle(List<Subtitle> subtitles) {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        for (Subtitle subtitle : subtitles) {
            String entry = String.format("%d\n%s --> %s\n%s", index++, parseDate(subtitle.getStartAt()), parseDate(subtitle.getEndAt()), subtitle.getText());
            builder.append(entry).append("\n");
        }

        return builder.toString();
    }
}
