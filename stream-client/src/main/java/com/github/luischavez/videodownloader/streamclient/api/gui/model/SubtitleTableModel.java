package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import com.github.luischavez.videodownloader.streamclient.api.Utils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SubtitleTableModel extends AbstractTableModel {

    public static final String[] COLUMNS = {"Start at", "End at", "Text"};
    public static final int[] COLUMN_WIDTHS = {100, 100, 250};
    public static final int[] COLUMN_MAX_WIDTHS = {100, 100, 0};

    private List<Subtitle> subtitles;
    private JTextField searchTextField;

    public SubtitleTableModel(List<Subtitle> subtitles, JTextField searchTextField) {
        this.subtitles = subtitles;
        this.searchTextField = searchTextField;
    }

    public void setSubtitles(List<Subtitle> subtitles) {
        this.subtitles = subtitles;

        fireTableDataChanged();
    }

    public List<Subtitle> subtitles() {
        return subtitles.stream()
                .filter(subtitle -> subtitle.getText().contains(searchTextField.getText()))
                .sorted(Comparator.comparing(Subtitle::getStartAt))
                .collect(Collectors.toList());
    }

    public int indexOf(Subtitle subtitle) {
        return subtitles.indexOf(subtitle);
    }

    public void clear() {
        subtitles.clear();

        fireTableDataChanged();
    }

    public void add(Subtitle subtitle) {
        subtitles.add(subtitle);

        fireTableDataChanged();
    }

    public void remove(Subtitle subtitle) {
        int indexOf = subtitles.indexOf(subtitle);
        subtitles.remove(subtitle);

        fireTableRowsDeleted(indexOf, indexOf);
    }

    public void remove(int index) {
        subtitles.remove(index);

        fireTableRowsDeleted(index, index);
    }

    @Override
    public int getRowCount() {
        return subtitles().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        List<Subtitle> subtitles = subtitles();

        if (rowIndex >= subtitles.size()) return null;

        Subtitle subtitle = subtitles.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return Utils.parseDate(subtitle.getStartAt());
            case 1:
                return Utils.parseDate(subtitle.getEndAt());
            case 2:
                return subtitle.getText();
            case 3:
                return "delete";

        }

        return null;
    }
}
