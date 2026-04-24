package com.github.luischavez.videodownloader.app.gui.model;

import com.github.luischavez.videodownloader.app.task.ClipTask;
import com.github.luischavez.videodownloader.app.util.FormatUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ClipTableModel extends AbstractTableModel {

    private final CopyOnWriteArrayList<ClipTask> tasks;

    public ClipTableModel() {
        tasks = new CopyOnWriteArrayList<>();
    }

    public void clear() {
        tasks.clear();

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public List<ClipTask> getTasks() {
        return tasks;
    }

    public int getIndex(ClipTask task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).equals(task)) return i;
        }

        return -1;
    }

    public ClipTask getAtRow(int row) {
        return tasks.get(row);
    }

    public void add(ClipTask task) {
        tasks.add(task);

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public void remove(ClipTask task) {
        tasks.remove(task);

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public void remove(File file) {
        List<ClipTask> toRemove = tasks.stream()
                .filter(task -> task.getFile().equals(file))
                .collect(Collectors.toList());

        tasks.removeAll(toRemove);

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    @Override
    public int getRowCount() {
        return tasks.size();
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 5 || columnIndex == 6;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 5 || columnIndex == 6) return super.getColumnClass(columnIndex);

        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 5) return "Jump to";
        if (columnIndex == 6) return "Remove";

        ClipTask task = tasks.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return task.getFile().getName();
            case 1:
                return FormatUtils.formatTime(task.getStartAt());
            case 2:
                return FormatUtils.formatTime(task.getStopAt());
            case 3:
                return FormatUtils.formatTime(task.getStopAt() - task.getStartAt());
            case 4:
                return task.getStatus();
        }

        return "";
    }
}
