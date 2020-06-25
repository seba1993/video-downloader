package com.github.luischavez.videodownloader.app.gui.model;

import com.github.luischavez.videodownloader.schedule.Schedule;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ScheduleTableModel extends AbstractTableModel {

    private final List<Schedule> schedules;

    public ScheduleTableModel(List<Schedule> schedules) {
        this.schedules = new ArrayList<>(schedules);
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void clear() {
        schedules.clear();

        fireTableDataChanged();
    }

    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);

        fireTableDataChanged();
    }

    public void removeSchedule(Schedule schedule) {
        schedules.remove(schedule);

        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return schedules.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        return "Schedule";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Schedule schedule = schedules.get(rowIndex);

        final String day = schedule.getDay().name();
        final String startAt = schedule.getStartAtTime().toString();
        final long minutes = schedule.getDuration() / Schedule.ONE_MINUTE;

        return String.format("%s starting at %s and stop after %d minutes", day, startAt, minutes);
    }
}
