package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TableSubtitleCellRenderer extends DefaultTableCellRenderer {

    public TableSubtitleCellRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String stringValue = value.toString()
                .replace("<shortcut>", "")
                .replace("</shortcut>", "");

        return super.getTableCellRendererComponent(table, stringValue, isSelected, hasFocus, row, column);
    }
}
