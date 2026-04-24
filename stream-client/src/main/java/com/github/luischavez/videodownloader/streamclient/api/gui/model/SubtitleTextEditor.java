package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class SubtitleTextEditor extends AbstractCellEditor implements TableCellEditor, KeyListener {

    final JScrollPane scrollPane = new JScrollPane();
    final JTextArea textArea = new JTextArea();

    public SubtitleTextEditor() {
        scrollPane.setViewportView(textArea);

        textArea.addKeyListener(this);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textArea.setText(value.toString());

        return scrollPane;
    }

    public boolean isCellEditable(EventObject evt) {
        if (evt instanceof MouseEvent) {
            return ((MouseEvent) evt).getClickCount() >= 2;
        }
        return true;
    }

    public Object getCellEditorValue() {
        return textArea.getText();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            stopCellEditing();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
