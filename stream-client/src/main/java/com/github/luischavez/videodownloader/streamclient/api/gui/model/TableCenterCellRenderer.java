package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class TableCenterCellRenderer extends DefaultTableCellRenderer {

    public TableCenterCellRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }
}
