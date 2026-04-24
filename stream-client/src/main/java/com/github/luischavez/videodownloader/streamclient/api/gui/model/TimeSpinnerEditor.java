package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import com.github.luischavez.videodownloader.streamclient.api.Utils;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.DateFormatter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.EventObject;

public class TimeSpinnerEditor extends AbstractCellEditor implements TableCellEditor {

    final JSpinner spinner = new JSpinner();

    public TimeSpinnerEditor() {
        SpinnerDateModel model = new SpinnerDateModel();
        spinner.setModel(model);

        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm:ss,SSS");
        DateFormatter formatter = (DateFormatter) editor.getTextField().getFormatter();
        formatter.setOverwriteMode(true);

        spinner.setEditor(editor);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Date date = Utils.parseDate(value.toString());

        spinner.setValue(date);

        return spinner;
    }

    public boolean isCellEditable(EventObject evt) {
        if (evt instanceof MouseEvent) {
            return ((MouseEvent) evt).getClickCount() >= 2;
        }
        return true;
    }

    public Object getCellEditorValue() {
        return Utils.parseDate((Date) spinner.getValue());
    }
}
