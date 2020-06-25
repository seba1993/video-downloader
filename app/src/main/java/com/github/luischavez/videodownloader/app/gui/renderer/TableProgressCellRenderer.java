package com.github.luischavez.videodownloader.app.gui.renderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TableProgressCellRenderer extends JProgressBar implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int progress = Float.valueOf(value.toString()).intValue();
        boolean undefined = progress == -1;
        progress = progress == -1 ? 100 : progress;

        setString(undefined ? "∞" : String.format("%d%%", progress));
        setStringPainted(true);
        setValue(progress);

        return this;
    }
}
