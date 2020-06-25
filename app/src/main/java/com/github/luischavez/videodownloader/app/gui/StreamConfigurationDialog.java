/*
 * Created by JFormDesigner on Tue Jun 23 08:13:12 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
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
    public StreamConfiguration streamConfiguration;

    public StreamConfigurationDialog(Window owner) {
        super(owner);

        initComponents();
        initialize();
    }

    private final void initialize() {
        scheduleTableModel = new ScheduleTableModel(new ArrayList<>());
        scheduleTable.setModel(scheduleTableModel);

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

    public void open(String alias) {
        if (isVisible()) return;

        streamConfiguration = AppContext.instance().getSystem().getManager(ConfigurationManager.class).list(StreamConfiguration.class).stream()
                .filter(streamConfiguration -> streamConfiguration.getAlias().equals(alias))
                .findFirst()
                .orElse(null);

        aliasTextField.setText("");
        urlTextField.setText("");
        fileNameTextField.setText("");
        scheduleTableModel.clear();

        setVisible(true);

        if (streamConfiguration == null) return;

        enableCheckBox.setSelected(streamConfiguration.isEnabled());
        typeComboBox.setSelectedItem(streamConfiguration.getType());
        aliasTextField.setText(streamConfiguration.getAlias());
        countryTextField.setText(streamConfiguration.getCountry());
        urlTextField.setText(streamConfiguration.getUrl());
        qualitySpinner.setValue(streamConfiguration.getPreferredQuality());
        fileNameTextField.setText(streamConfiguration.getBaseFileName());
        destinationTextField.setText(streamConfiguration.getDestinationPath());
        scheduleCheckBox.setSelected(streamConfiguration.isScheduleWhenAvailable());
        concatenateCheckBox.setSelected(streamConfiguration.isConcatenate());
        subCheckBox.setSelected(streamConfiguration.isSub());

        streamConfiguration.getSchedules().stream().forEach(scheduleTableModel::addSchedule);

        ArrayList<Integer> selectedSubs = new ArrayList<>();
        ListModel<String> subListModel = subList.getModel();
        for (int i = 0; i < subListModel.getSize(); i++) {
            if (streamConfiguration.getLanguages().contains(subListModel.getElementAt(i))) {
                selectedSubs.add(i);
            }
        }

        int[] selectedSubIndices = new int[selectedSubs.size()];
        for (int i = 0; i < selectedSubIndices.length; i++) {
            selectedSubIndices[i] = selectedSubs.get(i);
        }

        subList.setSelectedIndices(selectedSubIndices);
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
        label1 = new JLabel();
        aliasTextField = new JTextField();
        label2 = new JLabel();
        countryTextField = new JTextField();
        label3 = new JLabel();
        urlTextField = new JTextField();
        label13 = new JLabel();
        qualitySpinner = new JSpinner();
        label5 = new JLabel();
        fileNameTextField = new JTextField();
        label6 = new JLabel();
        destinationTextField = new JTextField();
        destinationButton = new JButton();
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
        label12 = new JLabel();
        concatenateTextField = new JTextField();
        concatenateDestinationButton = new JButton();
        subCheckBox = new JCheckBox();
        scrollPane2 = new JScrollPane();
        subList = new JList<>();
        vSpacer2 = new JPanel(null);

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "default, $lcgap, left:default, $lcgap, default:grow, 2*($lcgap, default)",
            "10*(default, $lgap), pref, 5*($lgap, default)"));
        contentPane.add(vSpacer1, CC.xywh(3, 1, 5, 1));

        //---- enableCheckBox ----
        enableCheckBox.setText("Enable");
        enableCheckBox.setSelected(true);
        contentPane.add(enableCheckBox, CC.xywh(3, 3, 3, 1));
        contentPane.add(hSpacer1, CC.xywh(1, 2, 1, 29));
        contentPane.add(hSpacer2, CC.xywh(9, 2, 1, 29));

        //---- saveButton ----
        saveButton.setText("Save");
        contentPane.add(saveButton, CC.xy(7, 3));

        //---- label4 ----
        label4.setText("Type");
        contentPane.add(label4, CC.xy(3, 5));

        //---- typeComboBox ----
        typeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
            "Video",
            "Audio"
        }));
        contentPane.add(typeComboBox, CC.xywh(5, 5, 3, 1));

        //---- label1 ----
        label1.setText("Alias");
        contentPane.add(label1, CC.xy(3, 7));
        contentPane.add(aliasTextField, CC.xywh(5, 7, 3, 1));

        //---- label2 ----
        label2.setText("Country");
        contentPane.add(label2, CC.xy(3, 9));
        contentPane.add(countryTextField, CC.xywh(5, 9, 3, 1));

        //---- label3 ----
        label3.setText("URL");
        contentPane.add(label3, CC.xy(3, 11));
        contentPane.add(urlTextField, CC.xywh(5, 11, 3, 1));

        //---- label13 ----
        label13.setText("Quality >=");
        contentPane.add(label13, CC.xy(3, 13));

        //---- qualitySpinner ----
        qualitySpinner.setModel(new SpinnerNumberModel(720, 480, null, 1));
        contentPane.add(qualitySpinner, CC.xy(5, 13, CC.LEFT, CC.DEFAULT));

        //---- label5 ----
        label5.setText("File Name");
        contentPane.add(label5, CC.xy(3, 15));
        contentPane.add(fileNameTextField, CC.xywh(5, 15, 3, 1));

        //---- label6 ----
        label6.setText("Destination");
        contentPane.add(label6, CC.xy(3, 17));

        //---- destinationTextField ----
        destinationTextField.setEditable(false);
        contentPane.add(destinationTextField, CC.xy(5, 17));

        //---- destinationButton ----
        destinationButton.setText("Browse");
        contentPane.add(destinationButton, CC.xy(7, 17));

        //---- scheduleCheckBox ----
        scheduleCheckBox.setText("Schedule When Available");
        contentPane.add(scheduleCheckBox, CC.xywh(3, 19, 3, 1));

        //======== panel1 ========
        {
            panel1. addPropertyChangeListener(new java.beans.PropertyChangeListener(){@Override public void propertyChange(java.beans.PropertyChangeEvent e
            ){if("borde\u0072".equals(e.getPropertyName()))throw new RuntimeException();}})
            ;
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
        contentPane.add(panel1, CC.xywh(3, 21, 5, 1));

        //---- concatenateCheckBox ----
        concatenateCheckBox.setText("Concatenate");
        contentPane.add(concatenateCheckBox, CC.xywh(3, 23, 3, 1));

        //---- label12 ----
        label12.setText("Destination");
        contentPane.add(label12, CC.xy(3, 25));

        //---- concatenateTextField ----
        concatenateTextField.setEditable(false);
        contentPane.add(concatenateTextField, CC.xy(5, 25));

        //---- concatenateDestinationButton ----
        concatenateDestinationButton.setText("Browse");
        contentPane.add(concatenateDestinationButton, CC.xy(7, 25));

        //---- subCheckBox ----
        subCheckBox.setText("Sub");
        contentPane.add(subCheckBox, CC.xywh(3, 27, 3, 1));

        //======== scrollPane2 ========
        {

            //---- subList ----
            subList.setModel(new AbstractListModel<String>() {
                String[] values = {
                    "Spanish",
                    "English",
                    "French",
                    "Italian"
                };
                @Override
                public int getSize() { return values.length; }
                @Override
                public String getElementAt(int i) { return values[i]; }
            });
            scrollPane2.setViewportView(subList);
        }
        contentPane.add(scrollPane2, CC.xywh(3, 29, 5, 1));
        contentPane.add(vSpacer2, CC.xywh(3, 31, 5, 1));
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
    private JLabel label1;
    public JTextField aliasTextField;
    private JLabel label2;
    public JTextField countryTextField;
    private JLabel label3;
    public JTextField urlTextField;
    private JLabel label13;
    public JSpinner qualitySpinner;
    private JLabel label5;
    public JTextField fileNameTextField;
    private JLabel label6;
    public JTextField destinationTextField;
    public JButton destinationButton;
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
    private JLabel label12;
    public JTextField concatenateTextField;
    public JButton concatenateDestinationButton;
    public JCheckBox subCheckBox;
    private JScrollPane scrollPane2;
    public JList<String> subList;
    private JPanel vSpacer2;
    private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
