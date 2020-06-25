package com.github.luischavez.videodownloader.app.gui.model;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.schedule.NeverSchedule;
import com.github.luischavez.videodownloader.schedule.Schedule;
import com.github.luischavez.videodownloader.schedule.ScheduleManager;
import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StreamTableModel extends AbstractTableModel {

    private final JTextField searchTextField;
    private final String[] columns;

    @Injected
    public StreamTableModel(JTextField searchTextField) {
        this.searchTextField = searchTextField;
        columns = new String[]{"Country", "Alias", "Status", "Download", "Time", "", "", ""};
    }

    protected List<StreamConfiguration> getStreamConfigurations() {
        List<StreamConfiguration> streamConfigurations = AppContext.instance().getSystem().getManager(ConfigurationManager.class)
                .list(StreamConfiguration.class)
                .stream()
                .filter(streamConfiguration -> streamConfiguration.getAlias().toLowerCase().contains(searchTextField.getText().toLowerCase()))
                .collect(Collectors.toList());

        streamConfigurations.sort((o1, o2) -> {
            final String s1 = o1.getCountry() + "-" + o1.getAlias();
            final String s2 = o2.getCountry() + "-" + o2.getAlias();

            return s1.compareToIgnoreCase(s2);
        });

        return streamConfigurations;
    }

    protected String buildStatusString(StreamConfiguration streamConfiguration, ScheduleManager.ScheduleEntry scheduleEntry, Task task) {
        if (!streamConfiguration.isEnabled()) return "disabled";
        if (task != null && task.isRunning()) return String.format("downloading (%s)", task.details());

        if (streamConfiguration.getSchedules().isEmpty() && !streamConfiguration.isScheduleWhenAvailable()) return "not scheduled";

        return "waiting";
    }

    protected String buildTimeLeft(long seconds, boolean start) {
        long minutes = 0;
        long hours = 0;
        long days = 0;

        if (seconds > 60) {
            minutes = seconds / 60;
            seconds %= 60;

            if (minutes > 60) {
                hours = minutes / 60;
                minutes %= 60;

                if (hours > 24) {
                    days = hours / 24;
                    hours %= 24;
                }
            }
        }

        ArrayList<String> parts = new ArrayList<>();

        parts.add(start ? "starting in" : "stopping in");

        if (days > 0) {
            parts.add(String.format("%d %s", days, days > 1 ? "days" : "day"));
        }

        if (hours > 0) {
            parts.add(String.format("%d %s", hours, hours > 1 ? "hours" : "hour"));
        }

        if (minutes > 0) {
            parts.add(String.format("%d %s", minutes, minutes > 1 ? "minutes" : "minute"));
        }

        if (seconds > 0) {
            parts.add(String.format("%d %s", seconds, seconds > 1 ? "seconds" : "seconds"));
        }

        return parts.stream().collect(Collectors.joining(" "));
    }

    protected String buildTimeString(StreamConfiguration streamConfiguration, ScheduleManager.ScheduleEntry scheduleEntry, Task task) {
        if (!streamConfiguration.isEnabled()) return "";
        if (task != null && task.isRunning() && streamConfiguration.isScheduleWhenAvailable()) return "∞";

        final Schedule schedule = scheduleEntry.getSchedule();

        if (schedule instanceof NeverSchedule) return "";

        final Schedule.ScheduleRange scheduleRange = schedule.calculateScheduleRange();

        final long secondsToStart = scheduleRange.timeToStart(ChronoUnit.SECONDS);
        final long secondsToStop = scheduleRange.timeToStop(ChronoUnit.SECONDS);

        if (task != null && task.isRunning() && secondsToStop > 0) return buildTimeLeft(secondsToStop, false);
        if ((task == null || !task.isRunning()) && secondsToStart > 0) return buildTimeLeft(secondsToStart, true);

        return "";
    }

    protected float calculateProgress(StreamConfiguration streamConfiguration, ScheduleManager.ScheduleEntry scheduleEntry, Task task) {
        if (task == null || !task.isRunning()) return 0f;
        if (streamConfiguration.isScheduleWhenAvailable()) return -1f;

        final Schedule schedule = scheduleEntry.getSchedule();
        final Schedule.ScheduleRange scheduleRange = schedule.calculateScheduleRange();

        final long secondsToStop = scheduleRange.timeToStop(ChronoUnit.SECONDS);

        return (secondsToStop * 100) / (schedule.getDuration() * 1_000f);
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3) return Float.class;

        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 5 || column == 6 || column == 7;
    }

    @Override
    public int getRowCount() {
        return getStreamConfigurations().size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        StreamConfiguration streamConfiguration = getStreamConfigurations().get(row);

        final long uid = streamConfiguration.uid();
        final String alias = streamConfiguration.getAlias();
        final String country = streamConfiguration.getCountry();

        ScheduleManager.ScheduleEntry scheduleEntry = AppContext.instance().getSystem().getManager(ScheduleManager.class).get(uid);
        Task task = AppContext.instance().getSystem().getManager(TaskManager.class).get(uid);

        final String status = buildStatusString(streamConfiguration, scheduleEntry, task);
        final String time = buildTimeString(streamConfiguration, scheduleEntry, task);
        final float progress = calculateProgress(streamConfiguration, scheduleEntry, task);
        final boolean isEnabled = streamConfiguration.isEnabled();

        switch (column) {
            case 0:
                return country;
            case 1:
                return alias;
            case 2:
                return status;
            case 3:
                return progress;
            case 4:
                return time;
            case 5:
                return "Configure";
            case 6:
                return isEnabled ? "Disable" : "Enable";
            case 7:
                return "Delete";
        }

        return null;
    }
}
