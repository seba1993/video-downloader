package com.github.luischavez.videodownloader.app.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.BaseManager;
import com.github.luischavez.videodownloader.system.Injected;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiRepaintManager extends BaseManager {

    private final List<Component> components;

    @Injected
    public GuiRepaintManager(Context context) {
        super(context);

        components = new ArrayList<>();
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void removeComponent(Component component) {
        components.remove(component);
    }

    @Override
    protected boolean doWork() throws Exception {
        components.stream()
                .forEach(component -> {
                    SwingUtilities.invokeLater(() -> {
                        if (component instanceof JTable) {
                            ((AbstractTableModel) JTable.class.cast(component).getModel()).fireTableDataChanged();
                        } else {
                            component.repaint();
                        }
                    });
                });

        return true;
    }
}
