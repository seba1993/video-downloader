package com.github.luischavez.videodownloader.app.gui.model;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.ScheduleFolderConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ScheduleFolderModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Folder", "Language", "Sub", "", ""};

    private final Context context;

    public ScheduleFolderModel(Context context) {
        this.context = context;
    }

    public List<ScheduleFolderConfiguration> getConfigurations() {
        return context.getSystem().getManager(ConfigurationManager.class)
                .list(ScheduleFolderConfiguration.class);
    }

    public ScheduleFolderConfiguration getConfigurationAt(int rowIndex) {
        List<ScheduleFolderConfiguration> configurations = getConfigurations();

        if (configurations.size() > rowIndex) {
            return configurations.get(rowIndex);
        }

        return null;
    }

    @Override
    public int getRowCount() {
        return getConfigurations().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3 || columnIndex == 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ScheduleFolderConfiguration configuration = getConfigurationAt(rowIndex);

        if (configuration != null) {
            switch (columnIndex) {
                case 0:
                    return configuration.getFolder();
                case 1:
                    return configuration.getLanguage();
                case 2:
                    return configuration.isSub();
                case 3:
                    return "Configure";
                case 4:
                    return "Remove";
            }
        }

        return null;
    }
}
