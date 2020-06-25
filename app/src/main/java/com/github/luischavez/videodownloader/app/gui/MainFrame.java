/*
 * Created by JFormDesigner on Mon Jun 22 17:33:33 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.BundleStreamConfiguration;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.gui.model.ScheduleTableModel;
import com.github.luischavez.videodownloader.app.gui.model.StreamTableModel;
import com.github.luischavez.videodownloader.app.gui.renderer.TableCenterCellRenderer;
import com.github.luischavez.videodownloader.app.gui.renderer.TableProgressCellRenderer;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResult;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResults;
import com.github.luischavez.videodownloader.schedule.Schedule;
import com.github.luischavez.videodownloader.util.PlatformUtils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author unknown
 */
public class MainFrame extends JFrame implements ActionListener, WindowListener {

    public static final String VERSION = "v2.0.0";
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String TITLE = String.format("M3U8 Downloader %s [Runtime %s]", VERSION, JAVA_VERSION);

    private TrayIcon trayIcon;

    private StreamConfigurationDialog streamConfigurationDialog;
    private LoadingDialog loadingDialog;

    public MainFrame() {
        initComponents();
    }

    private void changeStreamStatus(String alias, boolean enable) {
        ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

        StreamConfiguration streamConfiguration = configurationManager.list(StreamConfiguration.class).stream()
                .filter(s -> s.getAlias().equals(alias))
                .findFirst()
                .orElse(null);

        if (streamConfiguration == null) return;

        streamConfiguration.setEnabled(enable);
        configurationManager.add(streamConfiguration, true);
    }

    private void deleteStream(String alias) {
        ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

        StreamConfiguration streamConfiguration = configurationManager.list(StreamConfiguration.class).stream()
                .filter(s -> s.getAlias().equals(alias))
                .findFirst()
                .orElse(null);

        if (streamConfiguration == null) return;

        if (streamConfiguration.isEnabled()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Please disable the stream first", "Stream enabled", JOptionPane.WARNING_MESSAGE);
            });
            return;
        }

        configurationManager.remove(streamConfiguration);
    }

    private void changeStreamsStatus(boolean enable) {
        ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);
        List<StreamConfiguration> streamConfigurations = configurationManager.list(StreamConfiguration.class);

        streamConfigurations.stream().forEach(streamConfiguration -> {
            streamConfiguration.setEnabled(enable);
            configurationManager.add(streamConfiguration, true);
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == contentPanel.addButton) {
            streamConfigurationDialog.open(null);
        } else if (e.getSource() == contentPanel.enableAllButton) {
            SwingUtilities.invokeLater(() -> {
                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", "Enable all streams", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    changeStreamsStatus(true);
                }
            });
        } else if (e.getSource() == contentPanel.disableAllButton) {
            SwingUtilities.invokeLater(() -> {
                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", "Disable all streams", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    changeStreamsStatus(false);
                }
            });
        } else if (e.getSource() == streamConfigurationDialog.saveButton) {
            final StreamConfiguration streamConfiguration =
                    streamConfigurationDialog.streamConfiguration == null
                    ? new StreamConfiguration()
                    : new StreamConfiguration(streamConfigurationDialog.streamConfiguration.uid());

            streamConfiguration.setEnabled(streamConfigurationDialog.enableCheckBox.isSelected());
            streamConfiguration.setType(streamConfigurationDialog.typeComboBox.getSelectedItem().toString());
            streamConfiguration.setAlias(streamConfigurationDialog.aliasTextField.getText());
            streamConfiguration.setCountry(streamConfigurationDialog.countryTextField.getText());
            streamConfiguration.setUrl(streamConfigurationDialog.urlTextField.getText());
            streamConfiguration.setPreferredQuality(Integer.valueOf(streamConfigurationDialog.qualitySpinner.getValue().toString()));
            streamConfiguration.setBaseFileName(streamConfigurationDialog.fileNameTextField.getText());
            streamConfiguration.setDestinationPath(streamConfigurationDialog.destinationTextField.getText());
            streamConfiguration.setScheduleWhenAvailable(streamConfigurationDialog.scheduleCheckBox.isSelected());
            streamConfiguration.setSchedules(new ArrayList<>(streamConfigurationDialog.scheduleTableModel.getSchedules()));
            streamConfiguration.setConcatenate(streamConfigurationDialog.concatenateCheckBox.isSelected());
            streamConfiguration.setConcatenationPath(streamConfigurationDialog.concatenateTextField.getText());
            streamConfiguration.setSub(streamConfigurationDialog.subCheckBox.isSelected());
            streamConfiguration.setLanguages(streamConfigurationDialog.subList.getSelectedValuesList());

            final ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.setLocationRelativeTo(null);
                    loadingDialog.setVisible(true);
                });

                try {
                    final ValidationResults validationResults = configurationManager.validate(streamConfiguration);

                    if (validationResults.fails()) {
                        SwingUtilities.invokeLater(() -> {
                            JPanel validationPanel = new JPanel();
                            validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

                            for (ValidationResult validationResult : validationResults) {
                                validationPanel.add(new JLabel(validationResult.getMessage()));
                            }

                            loadingDialog.setVisible(false);
                            JOptionPane.showMessageDialog(streamConfigurationDialog, validationPanel, "ERROR!", JOptionPane.ERROR_MESSAGE);
                        });
                    } else {
                        configurationManager.add(streamConfiguration, true);

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            streamConfigurationDialog.setVisible(false);
                        });
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                }
            }).start();
        } else if (e.getSource() == exitMenuItem) {
            exit();
        } else if (e.getSource() == exportMenuItem) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            int option = chooser.showSaveDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = chooser.getSelectedFile();
                final String fileName = selectedFile.getName().split("\\.")[0] + ".streambundle";

                new Thread(() -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.setLocationRelativeTo(null);
                        loadingDialog.setVisible(true);
                    });

                    try {
                        BundleStreamConfiguration.store(selectedFile.getParentFile().getPath(), fileName);

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            JOptionPane.showMessageDialog(this, "information exported", "SUCCESS!", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "can't export the stream information", "ERROR!", JOptionPane.ERROR_MESSAGE));
                    } finally {
                        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                    }
                }).start();
            }
        } else if (e.getSource() == importMenuItem) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new FileNameExtensionFilter("stream bundle", "streambundle"));

            int option = chooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = chooser.getSelectedFile();

                new Thread(() -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.setLocationRelativeTo(null);
                        loadingDialog.setVisible(true);
                    });

                    try {
                        BundleStreamConfiguration bundleStreamConfiguration = BundleStreamConfiguration.read(selectedFile.getPath());

                        final ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

                        bundleStreamConfiguration.getStreamConfigurations().stream()
                                .forEach(streamConfiguration -> {
                                    if (configurationManager.validate(streamConfiguration).pass()) {
                                        configurationManager.add(streamConfiguration, true);
                                    }
                                });

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);

                            JOptionPane.showMessageDialog(this, "information imported", "SUCCESS!", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "can't import the stream information", "ERROR!", JOptionPane.ERROR_MESSAGE));
                    } finally {
                        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                    }
                }).start();
            }
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        exit();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        fromTray();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        toTray();
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    private TrayIcon createTrayIcon() {
        if (trayIcon != null) return trayIcon;

        PopupMenu popupMenu = new PopupMenu();

        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.addActionListener((actionEvent) -> fromTray());

        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.addActionListener((actionEvent) -> exit());

        popupMenu.add(openMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(exitMenuItem);

        Image icon = Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource(PlatformUtils.isWindowsHost() ? "/trayicon.gif" : "/trayicon.png"));

        trayIcon = new TrayIcon(icon, TITLE, popupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    fromTray();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        return trayIcon;
    }

    public void fromTray() {
        try {
            SystemTray.getSystemTray().remove(createTrayIcon());
            setState(Frame.NORMAL);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            pack();
            setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            AppContext.instance().error(MainFrame.class, "can't add tray icon", ex);
        }
    }

    public void toTray() {
        try {
            SystemTray.getSystemTray().add(createTrayIcon());
            setState(Frame.ICONIFIED);
            setVisible(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            AppContext.instance().error(MainFrame.class, "can't add tray icon", ex);
        }
    }

    public void exit() {
        int option = JOptionPane.showConfirmDialog(this,
                "Are you sure?", "Closing...",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.setLocationRelativeTo(null);
                    loadingDialog.setVisible(true);
                });

                AppContext.instance().getSystem().stopAllManagers(true);

                List<Long> pids = new ArrayList<>(RunningPids.load().pids());
                RunningPids.killAll();

                while (!pids.isEmpty()) {
                    List<Long> killed = pids.stream()
                            .filter(pid -> !PlatformUtils.isRunning(pid))
                            .collect(Collectors.toList());

                    pids.removeAll(killed);
                }

                System.exit(0);
            }).start();
        }
    }

    public void initialize() {
        addWindowListener(this);

        setAutoRequestFocus(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setVisible(true);

        streamConfigurationDialog = new StreamConfigurationDialog(this);
        loadingDialog = new LoadingDialog(this);

        contentPanel.addButton.addActionListener(this);
        contentPanel.enableAllButton.addActionListener(this);
        contentPanel.disableAllButton.addActionListener(this);

        contentPanel.searchTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    contentPanel.streamTable.repaint();
                    contentPanel.streamScrollPane.setViewportView(contentPanel.streamTable);
                });
            }
        });

        contentPanel.streamTable.setModel(new StreamTableModel(contentPanel.searchTextField));

        TableColumnModel columnModel = contentPanel.streamTable.getColumnModel();

        columnModel.getColumn(0).setMinWidth(80);
        columnModel.getColumn(0).setPreferredWidth(80);
        columnModel.getColumn(1).setMinWidth(100);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setMinWidth(300);
        columnModel.getColumn(2).setPreferredWidth(300);
        columnModel.getColumn(3).setMinWidth(150);
        columnModel.getColumn(3).setPreferredWidth(150);
        columnModel.getColumn(4).setMinWidth(150);
        columnModel.getColumn(4).setPreferredWidth(150);
        columnModel.getColumn(5).setMinWidth(70);
        columnModel.getColumn(5).setPreferredWidth(70);
        columnModel.getColumn(6).setMinWidth(70);
        columnModel.getColumn(6).setPreferredWidth(70);
        columnModel.getColumn(7).setMinWidth(70);
        columnModel.getColumn(7).setPreferredWidth(70);

        contentPanel.streamTable.getTableHeader().setReorderingAllowed(false);
        contentPanel.streamTable.getTableHeader().setResizingAllowed(false);

        contentPanel.streamTable.setDragEnabled(false);

        contentPanel.streamTable.setDefaultRenderer(String.class, new TableCenterCellRenderer());
        contentPanel.streamTable.setDefaultRenderer(Float.class, new TableProgressCellRenderer());

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 1).toString();

                streamConfigurationDialog.open(alias);
            }
        }, 5);

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 1).toString();
                final String changeToStatus = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 6).toString();

                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", String.format("%s %s stream", changeToStatus, alias), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    changeStreamStatus(alias, changeToStatus.equals("Enable"));
                }
            }
        }, 6);

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 1).toString();

                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", String.format("Delete %s stream", alias), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    deleteStream(alias);
                }
            }
        }, 7);

        streamConfigurationDialog.saveButton.addActionListener(this);

        exitMenuItem.addActionListener(this);
        exportMenuItem.addActionListener(this);
        importMenuItem.addActionListener(this);
        configurationMenuItem.addActionListener(this);

        streamConfigurationDialog.deleteMenuItem.addActionListener((actionEvent) -> {
            int row = streamConfigurationDialog.scheduleTable.getSelectedRow();

            Schedule schedule = ((ScheduleTableModel) streamConfigurationDialog.scheduleTable.getModel()).getSchedules().get(row);
            ((ScheduleTableModel) streamConfigurationDialog.scheduleTable.getModel()).removeSchedule(schedule);
        });

        streamConfigurationDialog.menu.add(streamConfigurationDialog.deleteMenuItem);

        streamConfigurationDialog.scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        streamConfigurationDialog.scheduleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = streamConfigurationDialog.scheduleTable.rowAtPoint(e.getPoint());

                    if (row > -1) {
                        streamConfigurationDialog.scheduleTable.changeSelection(row, 1, false, false);
                        streamConfigurationDialog.menu.show(streamConfigurationDialog.scheduleTable, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        menuBar = new JMenuBar();
        menu1 = new JMenu();
        configurationMenuItem = new JMenuItem();
        importMenuItem = new JMenuItem();
        exportMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        contentPanel = new ContentPanel();
        separator2 = new JSeparator();
        scrollPane1 = new JScrollPane();
        logTextArea = new JTextArea();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "default:grow",
            "2*(default), fill:40dlu:grow"));

        //======== menuBar ========
        {

            //======== menu1 ========
            {
                menu1.setText("File");

                //---- configurationMenuItem ----
                configurationMenuItem.setText("Configuration");
                menu1.add(configurationMenuItem);
                menu1.addSeparator();

                //---- importMenuItem ----
                importMenuItem.setText("Import");
                menu1.add(importMenuItem);

                //---- exportMenuItem ----
                exportMenuItem.setText("Export");
                menu1.add(exportMenuItem);
                menu1.addSeparator();

                //---- exitMenuItem ----
                exitMenuItem.setText("Exit");
                menu1.add(exitMenuItem);
            }
            menuBar.add(menu1);
        }
        setJMenuBar(menuBar);
        contentPane.add(contentPanel, CC.xy(1, 1));
        contentPane.add(separator2, CC.xy(1, 2));

        //======== scrollPane1 ========
        {

            //---- logTextArea ----
            logTextArea.setEditable(false);
            logTextArea.setBorder(LineBorder.createBlackLineBorder());
            scrollPane1.setViewportView(logTextArea);
        }
        contentPane.add(scrollPane1, CC.xy(1, 3));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JMenuBar menuBar;
    private JMenu menu1;
    public JMenuItem configurationMenuItem;
    public JMenuItem importMenuItem;
    public JMenuItem exportMenuItem;
    public JMenuItem exitMenuItem;
    public ContentPanel contentPanel;
    private JSeparator separator2;
    private JScrollPane scrollPane1;
    public JTextArea logTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
