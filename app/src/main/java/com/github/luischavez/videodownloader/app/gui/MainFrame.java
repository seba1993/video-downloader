/*
 * Created by JFormDesigner on Mon Jun 22 17:33:33 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.*;
import com.github.luischavez.videodownloader.app.gui.model.ScheduleTableModel;
import com.github.luischavez.videodownloader.app.gui.model.StreamTableModel;
import com.github.luischavez.videodownloader.app.gui.renderer.TableCenterCellRenderer;
import com.github.luischavez.videodownloader.app.gui.renderer.TableProgressCellRenderer;
import com.github.luischavez.videodownloader.app.manager.YouTubeManager;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.BaseContext;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResult;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResults;
import com.github.luischavez.videodownloader.schedule.Schedule;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.github.luischavez.videodownloader.util.PlatformUtils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author unknown
 */
public class MainFrame extends JFrame implements ActionListener, WindowListener {

    public static final String VERSION = "v2.4.1";
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String TITLE = String.format("M3U8 Downloader %s [Runtime %s]", VERSION, JAVA_VERSION);
    private static final String YT_DLP_CONFIG_NAME = "yt-dlp.conf";
    private static final String YOUTUBE_DL_CONFIG_NAME = "youtube-dl.conf";
    private static final String YT_DLP_CHANNELS_NAME = "yt-dlp-channels.txt";
    private static final String YOUTUBE_DL_CHANNELS_NAME = "youtube-dl-channels.txt";
    private static final String YT_DLP_ARCHIVE_NAME = "yt-dlp-archive.txt";

    private TrayIcon trayIcon;

    private AppConfigurationDialog appConfigurationDialog;
    private PathConfigurationDialog pathConfigurationDialog;
    private StreamConfigurationDialog streamConfigurationDialog;
    private MonitorConfigurationDialog monitorConfigurationDialog;
    private LoadingDialog loadingDialog;

    private int firstSelectedRow;
    private int lastSelectedRow;

    public StreamTableModel streamTableModel;

    public MainFrame() {
        initComponents();
    }

    private File resolveYouTubeFile(String preferredName, String legacyName) {
        File preferredFile = new File(AppContext.instance().buildPath(BaseContext.resolveWorkingDir(), "youtube", preferredName));
        if (preferredFile.exists()) return preferredFile;

        return new File(AppContext.instance().buildPath(BaseContext.resolveWorkingDir(), "youtube", legacyName));
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
            try {
                AppContext.instance().log(MainFrame.class, "debug", "add stream button clicked", null);
                streamConfigurationDialog.open((String[]) null);
            } catch (Throwable ex) {
                AppContext.instance().error(MainFrame.class, "can't open stream configuration dialog", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == contentPanel.ytButton) {
            YouTubeConfigurationPanel youTubeConfigurationPanel = new YouTubeConfigurationPanel();

            File channelsFile = resolveYouTubeFile(YT_DLP_CHANNELS_NAME, YOUTUBE_DL_CHANNELS_NAME);
            File confFile = resolveYouTubeFile(YT_DLP_CONFIG_NAME, YOUTUBE_DL_CONFIG_NAME);

            if (confFile.exists()) {
                try {
                    Files.lines(confFile.toPath())
                        .forEach(s -> {
                            if (s.startsWith("-o")) {
                                String dir = s.substring(4, s.indexOf("/%(uploader)s"));
                                youTubeConfigurationPanel.directoryTextField.setText(dir);
                            }
                        });
                } catch (Exception ex) {
                    AppContext.instance().error(MainFrame.class, ex.getMessage(), ex);
                }
            }

            if (channelsFile.exists()) {
                try {
                    String lines = Files.lines(channelsFile.toPath())
                            .collect(Collectors.joining("\n"));
                    youTubeConfigurationPanel.channelTextArea.setText(lines);
                } catch (Exception ex) {
                    AppContext.instance().error(MainFrame.class, ex.getMessage(), ex);
                }
            }

            int option = JOptionPane.showConfirmDialog(
                    this,
                    youTubeConfigurationPanel,
                    "YouTube",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                String conf = "-i\n" +
                        "-o \"" + youTubeConfigurationPanel.directoryTextField.getText() + "/%(uploader)s (%(uploader_id)s)/%(upload_date)s - %(title)s - (%(duration)ss) [%(resolution)s] [%(id)s].%(ext)s\"\n" +
                        "\n" +
                        "# Archive Settings\n" +
                        "--download-archive " + YT_DLP_ARCHIVE_NAME + "\n" +
                        "-a " + YT_DLP_CHANNELS_NAME + "\n" +
                        "\n" +
                        "# Uniform Format\n" +
                        "--prefer-ffmpeg\n" +
                        "--merge-output-format mkv\n" +
                        "\n" +
                        "# Get All Subs to SRT\n" +
                        "--write-sub\n" +
                        "--all-subs\n" +
                        "--convert-subs srt\n" +
                        "\n" +
                        "# Get metadata\n" +
                        "--add-metadata\n" +
                        "--write-description\n" +
                        "--write-thumbnail\n" +
                        "\n" +
                        "# Debug\n" +
                        "-v";

                try {
                    File youtubeDirectory = new File(AppContext.instance().buildPath(BaseContext.resolveWorkingDir(), "youtube"));
                    if (!youtubeDirectory.exists()) youtubeDirectory.mkdirs();

                    File newChannelsFile = new File(youtubeDirectory, YT_DLP_CHANNELS_NAME);
                    File newConfFile = new File(youtubeDirectory, YT_DLP_CONFIG_NAME);

                    if (newChannelsFile.exists()) newChannelsFile.delete();
                    if (newConfFile.exists()) newConfFile.delete();

                    Files.write(
                            newChannelsFile.toPath(),
                            youTubeConfigurationPanel.channelTextArea.getText().getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                    Files.write(
                            newConfFile.toPath(),
                            conf.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                    AppContext.instance()
                            .getSystem()
                            .getManager(YouTubeManager.class)
                            .restart();
                } catch (Exception ex) {
                    AppContext.instance().error(MainFrame.class, ex.getMessage(), ex);
                }
            }
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
        } else if (e.getSource() == appConfigurationDialog.saveButton) {
            final ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

            AppConfiguration appConfiguration = new AppConfiguration();

            appConfiguration.setEmail(appConfigurationDialog.emailTextField.getText());
            appConfiguration.setPassword(CryptoUtils.base64Encode(new String(appConfigurationDialog.passwordField.getPassword())));
            appConfiguration.setDistributionList(appConfigurationDialog.toTextField.getText().split(","));

            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.setLocationRelativeTo(null);
                    loadingDialog.setVisible(true);
                });

                try {
                    final ValidationResults validationResults = configurationManager.validate(appConfiguration);

                    if (validationResults.fails()) {
                        SwingUtilities.invokeLater(() -> {
                            JPanel validationPanel = new JPanel();
                            validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

                            for (ValidationResult validationResult : validationResults) {
                                validationPanel.add(new JLabel(validationResult.getMessage()));
                            }

                            loadingDialog.setVisible(false);
                            JOptionPane.showMessageDialog(appConfigurationDialog, validationPanel, "ERROR!", JOptionPane.ERROR_MESSAGE);
                        });
                    } else {
                        configurationManager.add(appConfiguration);

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            appConfigurationDialog.setVisible(false);
                        });
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                }
            }).start();
        } else if (e.getSource() == pathConfigurationDialog.saveButton) {
            final ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

            PathConfiguration pathConfiguration = new PathConfiguration();

            pathConfiguration.setAutosubPath(pathConfigurationDialog.autosubTextField.getText());

            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.setLocationRelativeTo(null);
                    loadingDialog.setVisible(true);
                });

                try {
                    final ValidationResults validationResults = configurationManager.validate(pathConfiguration);

                    if (validationResults.fails()) {
                        SwingUtilities.invokeLater(() -> {
                            JPanel validationPanel = new JPanel();
                            validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

                            for (ValidationResult validationResult : validationResults) {
                                validationPanel.add(new JLabel(validationResult.getMessage()));
                            }

                            loadingDialog.setVisible(false);
                            JOptionPane.showMessageDialog(pathConfigurationDialog, validationPanel, "ERROR!", JOptionPane.ERROR_MESSAGE);
                        });
                    } else {
                        configurationManager.add(pathConfiguration);

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            pathConfigurationDialog.setVisible(false);
                        });
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                }
            }).start();
        } else if (e.getSource() == streamConfigurationDialog.saveButton) {
            final boolean isMultiple = streamConfigurationDialog.isMultiple;
            final StreamConfiguration streamConfiguration =
                    streamConfigurationDialog.streamConfiguration == null
                    ? new StreamConfiguration()
                    : new StreamConfiguration(streamConfigurationDialog.streamConfiguration.uid());

            if (!isMultiple) {
                streamConfiguration.setAlias(streamConfigurationDialog.aliasTextField.getText());
                streamConfiguration.setUrl(streamConfigurationDialog.urlTextField.getText());
                streamConfiguration.setBaseFileName(streamConfigurationDialog.fileNameTextField.getText());
                streamConfiguration.setDestinationPath(streamConfigurationDialog.destinationTextField.getText());
            } else {
                streamConfiguration.setAlias("NEVER USED ALIAS");
                streamConfiguration.setUrl("http://placeholder.test");
                streamConfiguration.setBaseFileName("NEVER USED FILE NAME");
                streamConfiguration.setDestinationPath("NEVER USED DESTINATION PATH");
            }

            streamConfiguration.setEnabled(streamConfigurationDialog.enableCheckBox.isSelected());
            streamConfiguration.setType(streamConfigurationDialog.typeComboBox.getSelectedItem().toString());
            streamConfiguration.setCountry(streamConfigurationDialog.countryTextField.getText());
            streamConfiguration.setPreferredQuality(Integer.valueOf(streamConfigurationDialog.qualitySpinner.getValue().toString()));
            streamConfiguration.setTimeZoneId(streamConfigurationDialog.timeZoneComboBox.getSelectedItem().toString());
            streamConfiguration.setDailySplit(streamConfigurationDialog.dailySplitCheckBox.isSelected());
            streamConfiguration.setDailySplitAt(
                    LocalTime.of(
                            Integer.valueOf(streamConfigurationDialog.dailySplitHourSpinner.getValue().toString()),
                            Integer.valueOf(streamConfigurationDialog.dailySplitMinuteSpinner.getValue().toString())));
            streamConfiguration.setScheduleWhenAvailable(streamConfigurationDialog.scheduleCheckBox.isSelected());
            streamConfiguration.setSchedules(new ArrayList<>(streamConfigurationDialog.scheduleTableModel.getSchedules()));
            streamConfiguration.setConcatenate(streamConfigurationDialog.concatenateCheckBox.isSelected());
            streamConfiguration.setConcatenateAt(
                    LocalTime.of(
                            Integer.valueOf(streamConfigurationDialog.concatenateHourSpinner.getValue().toString()),
                            Integer.valueOf(streamConfigurationDialog.concatenateMinuteSpinner.getValue().toString())));
            streamConfiguration.setConcatenationPath(streamConfigurationDialog.concatenateTextField.getText());
            streamConfiguration.setSub(streamConfigurationDialog.subCheckBox.isSelected());
            streamConfiguration.setLanguage(streamConfigurationDialog.languageComboBox.getSelectedItem().toString());
            streamConfiguration.setTranslations(streamConfigurationDialog.subList.getSelectedValuesList());
            streamConfiguration.setDestinationPath(streamConfigurationDialog.destinationTextField.getText());

            if (isMultiple) {
                if (streamConfiguration.getCountry().isEmpty()) {
                    streamConfiguration.setCountry("NEVER USED COUNTRY");
                }
            }

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
                        if (!isMultiple) {
                            configurationManager.add(streamConfiguration, true);
                        } else {
                            final List<Integer> selectionList = streamTableModel.getSelectionList();
                            final List<String> aliases = selectionList.stream()
                                    .map(row -> streamTableModel.getValueAt(row, 2).toString())
                                    .collect(Collectors.toList());

                            final List<StreamConfiguration> selectedConfigurations = configurationManager.list(StreamConfiguration.class).stream()
                                    .filter(s -> aliases.contains(s.getAlias()))
                                    .collect(Collectors.toList());

                            selectedConfigurations.stream()
                                    .forEach(selectedConfiguration -> {
                                        String originalAlias = selectedConfiguration.getAlias();
                                        String originalUrl = selectedConfiguration.getUrl();
                                        String originalFileName = selectedConfiguration.getBaseFileName();
                                        String originalCountry = selectedConfiguration.getCountry();

                                        selectedConfiguration.copy(streamConfiguration);
                                        selectedConfiguration.setAlias(originalAlias);
                                        selectedConfiguration.setUrl(originalUrl);
                                        selectedConfiguration.setBaseFileName(originalFileName);

                                        if (selectedConfiguration.getCountry().equals("NEVER USED COUNTRY")) {
                                            selectedConfiguration.setCountry(originalCountry);
                                        }

                                        configurationManager.add(selectedConfiguration, true);
                                    });
                        }

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            streamConfigurationDialog.setVisible(false);
                            streamTableModel.deselectAll();
                            contentPanel.configureSelectedButton.setVisible(false);
                            contentPanel.deleteDisabled.setVisible(false);
                            contentPanel.selectionCheckBox.setSelected(false);
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
        } else if (e.getSource() == configurationMenuItem) {
            SwingUtilities.invokeLater(() -> appConfigurationDialog.open(AppContext.instance()));
        } else if (e.getSource() == contentPanel.configureSelectedButton) {
            final List<Integer> selectionList = streamTableModel.getSelectionList();
            final List<String> aliases = selectionList.stream()
                    .map(row -> streamTableModel.getValueAt(row, 2).toString())
                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> streamConfigurationDialog.open(aliases.toArray(new String[0])));
        } else if (e.getSource() == contentPanel.deleteDisabled) {
            final List<Integer> selectionList = streamTableModel.getSelectionList();
            final List<String> aliases = selectionList.stream()
                    .map(row -> streamTableModel.getValueAt(row, 2).toString())
                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> {
                streamTableModel.deselectAll();
                contentPanel.configureSelectedButton.setVisible(false);
                contentPanel.deleteDisabled.setVisible(false);
                contentPanel.selectionCheckBox.setSelected(false);
            });

            final List<String> disabledStreams = AppContext.instance().getSystem().getManager(ConfigurationManager.class)
                    .list(StreamConfiguration.class)
                    .stream()
                    .filter(streamConfiguration -> aliases.contains(streamConfiguration.getAlias()) && !streamConfiguration.isEnabled())
                    .map(streamConfiguration -> streamConfiguration.getAlias())
                    .collect(Collectors.toList());

            disabledStreams.forEach(this::deleteStream);
        } else if (e.getSource() == monitorMenuItem) {
            SwingUtilities.invokeLater(() -> monitorConfigurationDialog.open(AppContext.instance()));
        } else if (e.getSource() == monitorConfigurationDialog.saveButton) {
            final ConfigurationManager configurationManager = AppContext.instance().getSystem().getManager(ConfigurationManager.class);

            MonitorConfiguration monitorConfiguration = new MonitorConfiguration();

            monitorConfiguration.setEnabled(monitorConfigurationDialog.enableCheckBox.isSelected());
            monitorConfiguration.setUrl(monitorConfigurationDialog.urlTextField.getText());
            monitorConfiguration.setCode(monitorConfigurationDialog.codeTextField.getText());
            monitorConfiguration.setRefreshInterval(Integer.valueOf(monitorConfigurationDialog.refreshIntervalSpinner.getValue().toString()));

            new Thread(() -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.setLocationRelativeTo(null);
                    loadingDialog.setVisible(true);
                });

                try {
                    final ValidationResults validationResults = configurationManager.validate(monitorConfiguration);

                    if (validationResults.fails()) {
                        SwingUtilities.invokeLater(() -> {
                            JPanel validationPanel = new JPanel();
                            validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

                            for (ValidationResult validationResult : validationResults) {
                                validationPanel.add(new JLabel(validationResult.getMessage()));
                            }

                            loadingDialog.setVisible(false);
                            JOptionPane.showMessageDialog(monitorConfigurationDialog, validationPanel, "ERROR!", JOptionPane.ERROR_MESSAGE);
                        });
                    } else {
                        configurationManager.add(monitorConfiguration);

                        SwingUtilities.invokeLater(() -> {
                            loadingDialog.setVisible(false);
                            monitorConfigurationDialog.setVisible(false);
                        });
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
                }
            }).start();
        } else if (e.getSource() == pathMenuItem) {
            SwingUtilities.invokeLater(() -> pathConfigurationDialog.open(AppContext.instance()));
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

                AppContext.exiting.set(true);
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

        setTitle(TITLE);
        setAutoRequestFocus(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setVisible(true);

        appConfigurationDialog = new AppConfigurationDialog(this);
        pathConfigurationDialog = new PathConfigurationDialog(this);
        streamConfigurationDialog = new StreamConfigurationDialog(this);
        monitorConfigurationDialog = new MonitorConfigurationDialog(this);
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

        streamTableModel = new StreamTableModel(contentPanel.searchTextField);

        contentPanel.streamTable.setModel(streamTableModel);

        TableColumnModel columnModel = contentPanel.streamTable.getColumnModel();

        columnModel.getColumn(0).setMinWidth(50);
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setMinWidth(80);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setMinWidth(100);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setMinWidth(300);
        columnModel.getColumn(3).setPreferredWidth(300);
        columnModel.getColumn(4).setMinWidth(150);
        columnModel.getColumn(4).setPreferredWidth(150);
        columnModel.getColumn(5).setMinWidth(150);
        columnModel.getColumn(5).setPreferredWidth(150);
        columnModel.getColumn(6).setMinWidth(70);
        columnModel.getColumn(6).setPreferredWidth(70);
        columnModel.getColumn(7).setMinWidth(70);
        columnModel.getColumn(7).setPreferredWidth(70);
        columnModel.getColumn(8).setMinWidth(70);
        columnModel.getColumn(8).setPreferredWidth(70);

        contentPanel.streamTable.getTableHeader().setReorderingAllowed(false);
        contentPanel.streamTable.getTableHeader().setResizingAllowed(false);

        contentPanel.streamTable.setDragEnabled(false);

        contentPanel.streamTable.setDefaultRenderer(String.class, new TableCenterCellRenderer());
        contentPanel.streamTable.setDefaultRenderer(Float.class, new TableProgressCellRenderer());

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 2).toString();

                SwingUtilities.invokeLater(() -> streamConfigurationDialog.open(alias));
            }
        }, 6);

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 2).toString();
                final String changeToStatus = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 7).toString();

                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", String.format("%s %s stream", changeToStatus, alias), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    changeStreamStatus(alias, changeToStatus.equals("Enable"));
                }
            }
        }, 7);

        new ButtonColumn(contentPanel.streamTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String alias = contentPanel.streamTable.getValueAt(Integer.valueOf(e.getActionCommand()), 2).toString();

                int option = JOptionPane.showConfirmDialog(MainFrame.this, "are you sure?", String.format("Delete %s stream", alias), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    deleteStream(alias);
                }
            }
        }, 8);

        appConfigurationDialog.saveButton.addActionListener(this);
        pathConfigurationDialog.saveButton.addActionListener(this);
        streamConfigurationDialog.saveButton.addActionListener(this);
        monitorConfigurationDialog.saveButton.addActionListener(this);

        exitMenuItem.addActionListener(this);
        exportMenuItem.addActionListener(this);
        importMenuItem.addActionListener(this);
        configurationMenuItem.addActionListener(this);
        pathMenuItem.addActionListener(this);
        monitorMenuItem.addActionListener(this);

        contentPanel.ytButton.addActionListener(this);

        contentPanel.streamTable.getSelectionModel().addListSelectionListener(e -> {
            firstSelectedRow = e.getFirstIndex();
            lastSelectedRow = e.getLastIndex();
        });

        contentPanel.streamTable.getModel().addTableModelListener(e -> {
            SwingUtilities.invokeLater(() -> {
                if (firstSelectedRow >= 0) {
                    contentPanel.streamTable.setRowSelectionInterval(firstSelectedRow, lastSelectedRow);
                }
            });
        });

        contentPanel.streamTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int row = contentPanel.streamTable.rowAtPoint(e.getPoint());
                final int column = contentPanel.streamTable.columnAtPoint(e.getPoint());

                if (column == 0) {
                    streamTableModel.toggleSelection(row);

                    SwingUtilities.invokeLater(() -> {
                        boolean anySelected = !streamTableModel.getSelectionList().isEmpty();

                        contentPanel.configureSelectedButton.setVisible(anySelected);
                        contentPanel.deleteDisabled.setVisible(anySelected);
                    });
                }
            }
        });

        contentPanel.selectionCheckBox.addActionListener(e -> {
            final boolean selected = contentPanel.selectionCheckBox.isSelected();

            if (selected) {
                streamTableModel.selectAll();
            } else {
                streamTableModel.deselectAll();
            }

            SwingUtilities.invokeLater(() -> {
                contentPanel.configureSelectedButton.setVisible(selected);
                contentPanel.deleteDisabled.setVisible(selected);
                streamTableModel.fireTableDataChanged();
            });
        });

        contentPanel.configureSelectedButton.addActionListener(this);
        contentPanel.deleteDisabled.addActionListener(this);

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
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		menuBar = new JMenuBar();
		menu1 = new JMenu();
		configurationMenuItem = new JMenuItem();
		pathMenuItem = new JMenuItem();
		monitorMenuItem = new JMenuItem();
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

				//---- pathMenuItem ----
				pathMenuItem.setText("Path");
				menu1.add(pathMenuItem);

				//---- monitorMenuItem ----
				monitorMenuItem.setText("Monitor");
				menu1.add(monitorMenuItem);
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
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JMenuBar menuBar;
	private JMenu menu1;
	public JMenuItem configurationMenuItem;
	public JMenuItem pathMenuItem;
	public JMenuItem monitorMenuItem;
	public JMenuItem importMenuItem;
	public JMenuItem exportMenuItem;
	public JMenuItem exitMenuItem;
	public ContentPanel contentPanel;
	private JSeparator separator2;
	private JScrollPane scrollPane1;
	public JTextArea logTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
