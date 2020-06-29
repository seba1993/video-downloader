package com.github.luischavez.videodownloader.app.gui.model;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoTableModel extends AbstractTableModel {

    private final List<File> files;

    public VideoTableModel() {
        files = new ArrayList<>();
    }

    public void clear() {
        files.clear();

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public int getIndex(File file) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).equals(file)) return i;
        }

        return -1;
    }

    public File getFileAt(int row) {
        if (files.size() - 1 < row) return null;

        return files.get(row);
    }

    public void addAll(File... files) {
        this.files.addAll(Arrays.asList(files));

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public void addFile(File file) {
        files.add(file);

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    public void removeFile(File file) {
        files.remove(file);

        SwingUtilities.invokeLater(() -> fireTableDataChanged());
    }

    @Override
    public int getRowCount() {
        return files.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 1) return "Remove";

        return files.get(rowIndex).getName();
    }
}
