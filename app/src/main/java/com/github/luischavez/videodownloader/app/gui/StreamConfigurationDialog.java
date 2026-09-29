/*
 * Created by JFormDesigner on Tue Jun 23 08:13:12 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.table.*;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.gui.model.ScheduleTableModel;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.schedule.Schedule;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.jdesktop.beansbinding.*;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;

/**
 * @author unknown
 */
public class StreamConfigurationDialog extends JDialog {

    public final JPopupMenu menu = new JPopupMenu();
    public final JMenuItem deleteMenuItem = new JMenuItem("Delete");

    public ScheduleTableModel scheduleTableModel;

    public boolean isMultiple;
    public StreamConfiguration streamConfiguration;

    public StreamConfigurationDialog(Window owner) {
        super(owner);

        initComponents();
        initialize();
        setTitle("Stream Configuration");
    }

    private final void initialize() {
        scheduleTableModel = new ScheduleTableModel(new ArrayList<>());
        scheduleTable.setModel(scheduleTableModel);

        ArrayList<String> timeZones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(timeZones);
        timeZoneComboBox.setModel(new DefaultComboBoxModel<>(timeZones.toArray(new String[0])));
        timeZoneComboBox.setSelectedItem(ZoneId.systemDefault().getId());
        dailySplitCheckBox.addActionListener(actionEvent -> updateDailySplitControls());
        updateDailySplitControls();

        addScheduleButton.addActionListener((actionEvent) -> {
            final Schedule.Day day = Schedule.Day.valueOf(dayComboBox.getSelectedItem().toString());
            final LocalTime startAt = LocalTime.of(Integer.valueOf(hourSpinner.getValue().toString()), Integer.valueOf(minuteSpinner.getValue().toString()));
            final long duration = Integer.valueOf(durationSpinner.getValue().toString()) * Schedule.ONE_MINUTE;

            scheduleTableModel.addSchedule(new Schedule(day, startAt, duration));
        });

        destinationButton.addActionListener((actionEvent) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            int option = chooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                destinationTextField.setText(chooser.getSelectedFile().getPath());
            }
        });

        concatenateDestinationButton.addActionListener((actionEvent) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            int option = chooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                concatenateTextField.setText(chooser.getSelectedFile().getPath());
            }
        });
    }

    private void updateDailySplitControls() {
        boolean enabled = dailySplitCheckBox.isSelected();
        dailySplitHourSpinner.setEnabled(enabled);
        dailySplitMinuteSpinner.setEnabled(enabled);
    }

    public void open(String... alias) {
        if (isVisible()) return;

        streamConfiguration = null;

        isMultiple = alias != null && alias.length > 1;

        enableCheckBox.setSelected(!isMultiple);
        aliasLabel.setVisible(!isMultiple);
        aliasTextField.setVisible(!isMultiple);
        urlLabel.setVisible(!isMultiple);
        urlTextField.setVisible(!isMultiple);
        fileLabel.setVisible(!isMultiple);
        fileNameTextField.setVisible(!isMultiple);
        //destinationLabel.setVisible(!isMultiple);
        //destinationTextField.setVisible(!isMultiple);
        //destinationButton.setVisible(!isMultiple);

        aliasTextField.setText("");
        urlTextField.setText("");
        fileNameTextField.setText("");
        destinationTextField.setText("");
        timeZoneComboBox.setSelectedItem(ZoneId.systemDefault().getId());
        dailySplitCheckBox.setSelected(false);
        dailySplitHourSpinner.setValue(0);
        dailySplitMinuteSpinner.setValue(0);
        updateDailySplitControls();
        scheduleTableModel.clear();

        if (!isMultiple && alias != null) {
            streamConfiguration = AppContext.instance().getSystem().getManager(ConfigurationManager.class).list(StreamConfiguration.class).stream()
                    .filter(streamConfiguration -> streamConfiguration.getAlias().equals(alias[0]))
                    .findFirst()
                    .orElse(null);

            if (streamConfiguration == null) return;

            enableCheckBox.setSelected(streamConfiguration.isEnabled());
            typeComboBox.setSelectedItem(streamConfiguration.getType());
            aliasTextField.setText(streamConfiguration.getAlias());
            countryTextField.setText(streamConfiguration.getCountry());
            urlTextField.setText(streamConfiguration.getUrl());
            qualitySpinner.setValue(streamConfiguration.getPreferredQuality());
            timeZoneComboBox.setSelectedItem(streamConfiguration.getTimeZoneId());
            dailySplitCheckBox.setSelected(streamConfiguration.isDailySplit());
            dailySplitHourSpinner.setValue(streamConfiguration.getDailySplitAt().getHour());
            dailySplitMinuteSpinner.setValue(streamConfiguration.getDailySplitAt().getMinute());
            updateDailySplitControls();
            fileNameTextField.setText(streamConfiguration.getBaseFileName());
            destinationTextField.setText(streamConfiguration.getDestinationPath());
            scheduleCheckBox.setSelected(streamConfiguration.isScheduleWhenAvailable());
            concatenateCheckBox.setSelected(streamConfiguration.isConcatenate());
            concatenateHourSpinner.setValue(streamConfiguration.getConcatenateAt().getHour());
            concatenateMinuteSpinner.setValue(streamConfiguration.getConcatenateAt().getMinute());
            concatenateTextField.setText(streamConfiguration.getConcatenationPath());
            subCheckBox.setSelected(streamConfiguration.isSub());
            languageComboBox.setSelectedItem(streamConfiguration.getLanguage());

            streamConfiguration.getSchedules().stream().forEach(scheduleTableModel::addSchedule);

            ArrayList<Integer> selectedSubs = new ArrayList<>();
            ListModel<String> subListModel = subList.getModel();
            for (int i = 0; i < subListModel.getSize(); i++) {
                if (streamConfiguration.getTranslations().contains(subListModel.getElementAt(i))) {
                    selectedSubs.add(i);
                }
            }

            int[] selectedSubIndices = new int[selectedSubs.size()];
            for (int i = 0; i < selectedSubIndices.length; i++) {
                selectedSubIndices[i] = selectedSubs.get(i);
            }

            subList.setSelectedIndices(selectedSubIndices);
        } else {
            countryTextField.setText("");
        }

        pack();
        setLocationRelativeTo(getOwner());
        toFront();
        requestFocus();
        setVisible(true);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        vSpacer1 = new JPanel(null);
        enableCheckBox = new JCheckBox();
        hSpacer1 = new JPanel(null);
        hSpacer2 = new JPanel(null);
        saveButton = new JButton();
        label4 = new JLabel();
        typeComboBox = new JComboBox<>();
        aliasLabel = new JLabel();
        aliasTextField = new JTextField();
        label2 = new JLabel();
        countryTextField = new JTextField();
        urlLabel = new JLabel();
        urlTextField = new JTextField();
        label13 = new JLabel();
        qualitySpinner = new JSpinner();
        fileLabel = new JLabel();
        fileNameTextField = new JTextField();
        destinationLabel = new JLabel();
        destinationTextField = new JTextField();
        destinationButton = new JButton();
        timeZoneLabel = new JLabel();
        timeZoneComboBox = new JComboBox<>();
        dailySplitCheckBox = new JCheckBox();
        dailySplitAtLabel = new JLabel();
        dailySplitHourSpinner = new JSpinner();
        dailySplitSeparatorLabel = new JLabel();
        dailySplitMinuteSpinner = new JSpinner();
        scheduleCheckBox = new JCheckBox();
        panel1 = new JPanel();
        vSpacer3 = new JPanel(null);
        hSpacer3 = new JPanel(null);
        label7 = new JLabel();
        dayComboBox = new JComboBox<>();
        hSpacer5 = new JPanel(null);
        label8 = new JLabel();
        hourSpinner = new JSpinner();
        label9 = new JLabel();
        minuteSpinner = new JSpinner();
        hSpacer6 = new JPanel(null);
        label10 = new JLabel();
        durationSpinner = new JSpinner();
        label11 = new JLabel();
        hSpacer7 = new JPanel(null);
        addScheduleButton = new JButton();
        hSpacer4 = new JPanel(null);
        scrollPane1 = new JScrollPane();
        scheduleTable = new JTable();
        vSpacer4 = new JPanel(null);
        concatenateCheckBox = new JCheckBox();
        label15 = new JLabel();
        concatenateHourSpinner = new JSpinner();
        label16 = new JLabel();
        concatenateMinuteSpinner = new JSpinner();
        label12 = new JLabel();
        concatenateTextField = new JTextField();
        concatenateDestinationButton = new JButton();
        subCheckBox = new JCheckBox();
        label14 = new JLabel();
        languageComboBox = new JComboBox<>();
        scrollPane2 = new JScrollPane();
        subList = new JList<>();
        vSpacer2 = new JPanel(null);

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "default, $lcgap, left:default, $lcgap, default, $lcgap, center:[4dlu,min], $lcgap, default, $lcgap, default:grow, 2*($lcgap, default)",
            "12*(default, $lgap), pref, 7*($lgap, default)"));
        contentPane.add(vSpacer1, CC.xywh(3, 1, 11, 1));

        //---- enableCheckBox ----
        enableCheckBox.setText("Enable");
        enableCheckBox.setSelected(true);
        contentPane.add(enableCheckBox, CC.xywh(3, 3, 9, 1));
        contentPane.add(hSpacer1, CC.xywh(1, 2, 1, 37));
        contentPane.add(hSpacer2, CC.xywh(15, 2, 1, 37));

        //---- saveButton ----
        saveButton.setText("Save");
        contentPane.add(saveButton, CC.xy(13, 3));

        //---- label4 ----
        label4.setText("Type");
        contentPane.add(label4, CC.xy(3, 5));

        //---- typeComboBox ----
        typeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
            "Video",
            "Audio"
        }));
        contentPane.add(typeComboBox, CC.xywh(5, 5, 9, 1));

        //---- aliasLabel ----
        aliasLabel.setText("Alias");
        contentPane.add(aliasLabel, CC.xy(3, 7));
        contentPane.add(aliasTextField, CC.xywh(5, 7, 9, 1));

        //---- label2 ----
        label2.setText("Country");
        contentPane.add(label2, CC.xy(3, 9));
        contentPane.add(countryTextField, CC.xywh(5, 9, 9, 1));

        //---- urlLabel ----
        urlLabel.setText("URL");
        contentPane.add(urlLabel, CC.xy(3, 11));
        contentPane.add(urlTextField, CC.xywh(5, 11, 9, 1));

        //---- label13 ----
        label13.setText("Quality (p) >=");
        contentPane.add(label13, CC.xy(3, 13));

        //---- qualitySpinner ----
        qualitySpinner.setModel(new SpinnerNumberModel(720, 144, null, 1));
        contentPane.add(qualitySpinner, CC.xywh(5, 13, 7, 1, CC.LEFT, CC.DEFAULT));

        //---- fileLabel ----
        fileLabel.setText("File Name");
        contentPane.add(fileLabel, CC.xy(3, 15));
        contentPane.add(fileNameTextField, CC.xywh(5, 15, 9, 1));

        //---- destinationLabel ----
        destinationLabel.setText("Destination");
        contentPane.add(destinationLabel, CC.xy(3, 17));
        contentPane.add(destinationTextField, CC.xywh(5, 17, 7, 1));

        //---- destinationButton ----
        destinationButton.setText("Browse");
        contentPane.add(destinationButton, CC.xy(13, 17));

        //---- timeZoneLabel ----
        timeZoneLabel.setText("Time Zone");
        contentPane.add(timeZoneLabel, CC.xy(3, 19));
        contentPane.add(timeZoneComboBox, CC.xywh(5, 19, 9, 1));

        //---- dailySplitCheckBox ----
        dailySplitCheckBox.setText("Split Daily");
        contentPane.add(dailySplitCheckBox, CC.xywh(3, 21, 3, 1));

        //---- dailySplitAtLabel ----
        dailySplitAtLabel.setText("Cut At");
        contentPane.add(dailySplitAtLabel, CC.xy(7, 21));

        //---- dailySplitHourSpinner ----
        dailySplitHourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
        contentPane.add(dailySplitHourSpinner, CC.xy(9, 21));

        //---- dailySplitSeparatorLabel ----
        dailySplitSeparatorLabel.setText(":");
        contentPane.add(dailySplitSeparatorLabel, CC.xy(11, 21, CC.CENTER, CC.DEFAULT));

        //---- dailySplitMinuteSpinner ----
        dailySplitMinuteSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        contentPane.add(dailySplitMinuteSpinner, CC.xy(13, 21));

        //---- scheduleCheckBox ----
        scheduleCheckBox.setText("Schedule When Available");
        contentPane.add(scheduleCheckBox, CC.xywh(3, 23, 9, 1));

        //======== panel1 ========
        {
            panel1.setLayout(new FormLayout(
                "6*(default, $lcgap), center:[4dlu,min], 4*($lcgap, default), $lcgap, left:default, 2*($lcgap, default), $lcgap, default:grow, $lcgap, default",
                "2*(default, $lgap), 84dlu, $lgap, default"));
            panel1.add(vSpacer3, CC.xywh(3, 1, 27, 1));
            panel1.add(hSpacer3, CC.xywh(1, 3, 1, 4));

            //---- label7 ----
            label7.setText("Day");
            panel1.add(label7, CC.xy(3, 3));

            //---- dayComboBox ----
            dayComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                "Everyday",
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Sunday"
            }));
            panel1.add(dayComboBox, CC.xy(5, 3));
            panel1.add(hSpacer5, CC.xy(7, 3));

            //---- label8 ----
            label8.setText("Start At");
            panel1.add(label8, CC.xy(9, 3));

            //---- hourSpinner ----
            hourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
            panel1.add(hourSpinner, CC.xy(11, 3));

            //---- label9 ----
            label9.setText(":");
            panel1.add(label9, CC.xy(13, 3));

            //---- minuteSpinner ----
            minuteSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
            panel1.add(minuteSpinner, CC.xy(15, 3));
            panel1.add(hSpacer6, CC.xy(17, 3));

            //---- label10 ----
            label10.setText("Duration");
            panel1.add(label10, CC.xy(19, 3));

            //---- durationSpinner ----
            durationSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
            panel1.add(durationSpinner, CC.xy(21, 3));

            //---- label11 ----
            label11.setText("minutes");
            panel1.add(label11, CC.xy(23, 3));
            panel1.add(hSpacer7, CC.xy(25, 3));

            //---- addScheduleButton ----
            addScheduleButton.setText("Add");
            panel1.add(addScheduleButton, CC.xy(27, 3));
            panel1.add(hSpacer4, CC.xywh(31, 3, 1, 4));

            //======== scrollPane1 ========
            {

                //---- scheduleTable ----
                scheduleTable.setModel(new DefaultTableModel(
                    new Object[][] {
                    },
                    new String[] {
                        "Schedule"
                    }
                ) {
                    boolean[] columnEditable = new boolean[] {
                        false
                    };
                    @Override
                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return columnEditable[columnIndex];
                    }
                });
                {
                    TableColumnModel cm = scheduleTable.getColumnModel();
                    cm.getColumn(0).setResizable(false);
                }
                scrollPane1.setViewportView(scheduleTable);
            }
            panel1.add(scrollPane1, CC.xywh(3, 5, 27, 1));
            panel1.add(vSpacer4, CC.xywh(3, 7, 27, 1));
        }
        contentPane.add(panel1, CC.xywh(3, 25, 11, 1));

        //---- concatenateCheckBox ----
        concatenateCheckBox.setText("Concatenate");
        contentPane.add(concatenateCheckBox, CC.xywh(3, 27, 9, 1));

        //---- label15 ----
        label15.setText("Start At");
        contentPane.add(label15, CC.xy(3, 29));

        //---- concatenateHourSpinner ----
        concatenateHourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
        contentPane.add(concatenateHourSpinner, CC.xy(5, 29));

        //---- label16 ----
        label16.setText(":");
        contentPane.add(label16, CC.xy(7, 29, CC.CENTER, CC.DEFAULT));

        //---- concatenateMinuteSpinner ----
        concatenateMinuteSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        contentPane.add(concatenateMinuteSpinner, CC.xy(9, 29));

        //---- label12 ----
        label12.setText("Destination");
        contentPane.add(label12, CC.xy(3, 31));

        //---- concatenateTextField ----
        concatenateTextField.setEditable(false);
        contentPane.add(concatenateTextField, CC.xywh(5, 31, 7, 1));

        //---- concatenateDestinationButton ----
        concatenateDestinationButton.setText("Browse");
        contentPane.add(concatenateDestinationButton, CC.xy(13, 31));

        //---- subCheckBox ----
        subCheckBox.setText("Sub");
        contentPane.add(subCheckBox, CC.xywh(3, 33, 9, 1));

        //---- label14 ----
        label14.setText("Language");
        contentPane.add(label14, CC.xy(3, 35));

        //---- languageComboBox ----
        languageComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
            "Spanish",
            "English",
            "French",
            "Portuguese"
        }));
        contentPane.add(languageComboBox, CC.xywh(5, 35, 7, 1, CC.LEFT, CC.DEFAULT));

        //======== scrollPane2 ========
        {
            scrollPane2.setVisible(false);

            //---- subList ----
            subList.setModel(new AbstractListModel<String>() {
                String[] values = {
                    "Spanish",
                    "English",
                    "French",
                    "Portuguese"
                };
                @Override
                public int getSize() { return values.length; }
                @Override
                public String getElementAt(int i) { return values[i]; }
            });
            scrollPane2.setViewportView(subList);
        }
        contentPane.add(scrollPane2, CC.xywh(3, 37, 11, 1));
        contentPane.add(vSpacer2, CC.xywh(3, 39, 11, 1));
        pack();
        setLocationRelativeTo(getOwner());

        //---- bindings ----
        bindingGroup = new BindingGroup();
        bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            scheduleCheckBox, ELProperty.create("${!selected}"),
            panel1, BeanProperty.create("visible")));
        bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            concatenateCheckBox, BeanProperty.create("selected"),
            concatenateDestinationButton, BeanProperty.create("enabled")));
        bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            subCheckBox, BeanProperty.create("selected"),
            subList, BeanProperty.create("enabled")));
        bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
            concatenateCheckBox, BeanProperty.create("selected"),
            concatenateHourSpinner, BeanProperty.create("enabled")));
        bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            concatenateCheckBox, BeanProperty.create("selected"),
            concatenateMinuteSpinner, BeanProperty.create("enabled")));
        bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel vSpacer1;
    public JCheckBox enableCheckBox;
    private JPanel hSpacer1;
    private JPanel hSpacer2;
    public JButton saveButton;
    private JLabel label4;
    public JComboBox<String> typeComboBox;
    private JLabel aliasLabel;
    public JTextField aliasTextField;
    private JLabel label2;
    public JTextField countryTextField;
    private JLabel urlLabel;
    public JTextField urlTextField;
    private JLabel label13;
    public JSpinner qualitySpinner;
    private JLabel fileLabel;
    public JTextField fileNameTextField;
    private JLabel destinationLabel;
    public JTextField destinationTextField;
    public JButton destinationButton;
    private JLabel timeZoneLabel;
    public JComboBox<String> timeZoneComboBox;
    public JCheckBox dailySplitCheckBox;
    private JLabel dailySplitAtLabel;
    public JSpinner dailySplitHourSpinner;
    private JLabel dailySplitSeparatorLabel;
    public JSpinner dailySplitMinuteSpinner;
    public JCheckBox scheduleCheckBox;
    private JPanel panel1;
    private JPanel vSpacer3;
    private JPanel hSpacer3;
    private JLabel label7;
    private JComboBox<String> dayComboBox;
    private JPanel hSpacer5;
    private JLabel label8;
    private JSpinner hourSpinner;
    private JLabel label9;
    private JSpinner minuteSpinner;
    private JPanel hSpacer6;
    private JLabel label10;
    private JSpinner durationSpinner;
    private JLabel label11;
    private JPanel hSpacer7;
    private JButton addScheduleButton;
    private JPanel hSpacer4;
    private JScrollPane scrollPane1;
    public JTable scheduleTable;
    private JPanel vSpacer4;
    public JCheckBox concatenateCheckBox;
    private JLabel label15;
    public JSpinner concatenateHourSpinner;
    private JLabel label16;
    public JSpinner concatenateMinuteSpinner;
    private JLabel label12;
    public JTextField concatenateTextField;
    public JButton concatenateDestinationButton;
    public JCheckBox subCheckBox;
    private JLabel label14;
    public JComboBox<String> languageComboBox;
    private JScrollPane scrollPane2;
    public JList<String> subList;
    private JPanel vSpacer2;
    private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
