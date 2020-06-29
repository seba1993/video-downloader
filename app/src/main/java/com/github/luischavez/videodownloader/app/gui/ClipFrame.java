/*
 * Created by JFormDesigner on Fri Jun 26 08:47:20 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.gui.model.ClipTableModel;
import com.github.luischavez.videodownloader.app.gui.model.VideoTableModel;
import com.github.luischavez.videodownloader.app.gui.renderer.TableCenterCellRenderer;
import com.github.luischavez.videodownloader.app.task.ClipTask;
import com.github.luischavez.videodownloader.app.util.FormatUtils;
import com.github.luischavez.videodownloader.task.TaskManager;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.*;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * @author unknown
 */
public class ClipFrame extends JFrame implements
        ActionListener, ListSelectionListener, MouseListener, MouseMotionListener, ChangeListener,
        WindowListener, AWTEventListener, MediaEventListener, MediaPlayerEventListener {

    private final Context context;

    private final Canvas videoSurfaceCanvas;
    private final JFileChooser fileChooser;

    private final VideoTableModel videoTableModel;
    private final ClipTableModel clipTableModel;

    private EmbeddedMediaPlayer mediaPlayer;

    private File currentFile;

    private boolean clipStarted;
    private long clipStartAt;
    private long clipStopAt;

    private boolean timeSliding;

    private long lastTimeLeftArrowIncremented;
    private long lastTimeRightArrowIncremented;

    private long velocity;

    public ClipFrame(Context context) {
        this.context = context;

        initComponents();

        videoSurfaceCanvas = new Canvas();
        fileChooser = new JFileChooser();

        videoTableModel = new VideoTableModel();
        clipTableModel = new ClipTableModel();

        initialize();
    }

    private final void initialize() {
        /**
         * START MEDIA PLAYER
         */
        videoSurfaceCanvas.setBackground(Color.BLACK);

        videoPanel.setLayout(new CardLayout());
        videoPanel.add(videoSurfaceCanvas);

        List<String> vlcArgs = new ArrayList<>();

        vlcArgs.add("--no-snapshot-preview");
        vlcArgs.add("--quiet");

        MediaPlayerFactory playerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));

        mediaPlayer = playerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.videoSurface().set(playerFactory.videoSurfaces().newVideoSurface(videoSurfaceCanvas));

        /**
         * ADD LISTENERS
         */
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        playButton.addActionListener(this);
        loadButton.addActionListener(this);
        clipButton.addActionListener(this);
        startClipButton.addActionListener(this);
        mediaPlayer.events().addMediaPlayerEventListener(this);
        timeSlider.addMouseListener(this);
        timeSlider.addMouseMotionListener(this);
        timeSlider.addChangeListener(this);

        videoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel selectionModel = videoTable.getSelectionModel();

        selectionModel.addListSelectionListener(this);

        /**
         * STYLE
         */
        timeSlider.setMajorTickSpacing(1_000);
        timeSlider.setLabelTable(null);

        /**
         * TABLES
         */
        videoTable.getTableHeader().setReorderingAllowed(false);
        clipTable.getTableHeader().setReorderingAllowed(false);

        videoTable.setModel(videoTableModel);
        clipTable.setModel(clipTableModel);

        clipTable.setDefaultRenderer(String.class, new TableCenterCellRenderer());

        videoTable.setSelectionBackground(Color.GREEN);
        videoTable.setSelectionForeground(Color.WHITE);

        TableColumnModel videoTableColumnModel = videoTable.getColumnModel();
        videoTableColumnModel.getColumn(0).setHeaderValue("Video");
        videoTableColumnModel.getColumn(0).setMinWidth(200);
        videoTableColumnModel.getColumn(1).setHeaderValue("");
        videoTableColumnModel.getColumn(1).setMinWidth(50);

        TableColumnModel clipTableColumnModel = clipTable.getColumnModel();
        clipTableColumnModel.getColumn(0).setHeaderValue("Video");
        clipTableColumnModel.getColumn(0).setMinWidth(200);
        clipTableColumnModel.getColumn(1).setHeaderValue("From");
        clipTableColumnModel.getColumn(1).setMinWidth(100);
        clipTableColumnModel.getColumn(2).setHeaderValue("To");
        clipTableColumnModel.getColumn(2).setMinWidth(100);
        clipTableColumnModel.getColumn(3).setHeaderValue("Length");
        clipTableColumnModel.getColumn(3).setMinWidth(100);
        clipTableColumnModel.getColumn(4).setHeaderValue("Status");
        clipTableColumnModel.getColumn(4).setMinWidth(80);
        clipTableColumnModel.getColumn(5).setHeaderValue("");
        clipTableColumnModel.getColumn(5).setMinWidth(50);
        clipTableColumnModel.getColumn(6).setHeaderValue("");
        clipTableColumnModel.getColumn(6).setMinWidth(50);

        new ButtonColumn(videoTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                e.setSource("video_remove");
                ClipFrame.this.actionPerformed(e);
            }
        }, 1);

        new ButtonColumn(clipTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                e.setSource("jump_to");
                ClipFrame.this.actionPerformed(e);
            }
        }, 5);

        new ButtonColumn(clipTable, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                e.setSource("clip_remove");
                ClipFrame.this.actionPerformed(e);
            }
        }, 6);
    }

    private void setEnableStatusWhenClip(boolean status) {
        videoTable.setEnabled(status);
        clipTable.setEnabled(status);
        loadButton.setEnabled(status);
        startClipButton.setEnabled(status);
        beforeSpinner.setEnabled(status);
        afterSpinner.setEnabled(status);
        //playButton.setEnabled(status);
        //timeSlider.setEnabled(status);
    }

    private void loadVideo(File file) {
        if (currentFile != null && currentFile.equals(file)) {
            if (!mediaPlayer.status().isPlaying()) mediaPlayer.controls().play();
            return;
        }

        currentFile = file;

        mediaPlayer.media().play(file.getPath());
        videoTitleLabel.setText(file.getName());
        timeSlider.setEnabled(true);
        playButton.setEnabled(true);
        clipButton.setEnabled(true);
    }

    private void removeVideo() {
        if (currentFile == null) return;

        mediaPlayer.controls().stop();
        videoTitleLabel.setText("");
        timeSlider.setEnabled(false);
        playButton.setEnabled(false);
        clipButton.setEnabled(false);

        currentFile = null;
    }

    private void handleArrowKey(boolean pressed, boolean right) {
        if (currentFile == null) return;

        if (!pressed) {
            velocity = 0;
            lastTimeRightArrowIncremented = 0;
            lastTimeLeftArrowIncremented = 0;

            if (!mediaPlayer.status().isPlaying()) mediaPlayer.controls().play();

            return;
        }

        if (mediaPlayer.status().isPlaying()) mediaPlayer.controls().pause();

        long now = System.currentTimeMillis();
        boolean increment = false;

        if (right) {
            if (lastTimeRightArrowIncremented == 0 || (now - lastTimeRightArrowIncremented) >= 1_000L) {
                lastTimeRightArrowIncremented = now;
                increment = true;
            }
        } else {
            if (lastTimeLeftArrowIncremented == 0 || (now - lastTimeLeftArrowIncremented) >= 1_000L) {
                lastTimeLeftArrowIncremented = now;
                increment = true;
            }
        }

        if (increment) {
            velocity += 500L;
        }

        mediaPlayer.controls().skipTime(velocity * (right ? 1 : -1));
        handleTimeChanged(mediaPlayer.status().time());
    }

    private void handleRightArrowKey(boolean pressed) {
        handleArrowKey(pressed, true);
    }

    private void handleLeftArrowKey(boolean pressed) {
        handleArrowKey(pressed, false);
    }

    private void handleVideoRemove(int row) {
        final File file = videoTableModel.getFileAt(row);

        int option = JOptionPane.showConfirmDialog(
                ClipFrame.this,
                "all related clips will be removed, are you sure?",
                String.format("Remove %s", file.getName()),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            videoTableModel.removeFile(file);
            clipTableModel.remove(file);
            if (currentFile != null && currentFile == file) {
                removeVideo();
            }
        }
    }

    private void handleClipRemove(int row) {
        final ClipTask task = clipTableModel.getAtRow(row);

        int option = JOptionPane.showConfirmDialog(ClipFrame.this, "are you sure?", "Remove clio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            clipTableModel.remove(task);
        }
    }

    private void handleJumpTo(int row) {
        ClipTask task = clipTableModel.getAtRow(row);

        final int videoRow = videoTableModel.getIndex(task.getFile());

        SwingUtilities.invokeLater(() -> {
            loadVideo(task.getFile());
            mediaPlayer.controls().setTime(task.getStartAt());
            videoTable.changeSelection(videoRow, 0, false, false);
        });
    }

    private void handleLoad() {
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("MKV Video", "mkv"));

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            videoTableModel.addAll(selectedFiles);
        }
    }

    private void handleVideoSelection(int row) {
        if (!videoTable.isEnabled()) return;

        File file = videoTableModel.getFileAt(row);

        if (file == null) return;

        loadVideo(file);
    }

    private void handlePlayPause() {
        String toState = playButton.getText();

        if (toState.equals("Play")) {
            mediaPlayer.controls().play();
        } else if (toState.equals("Replay")) {
            mediaPlayer.controls().setTime(0);
            mediaPlayer.controls().play();
        } else {
            mediaPlayer.controls().pause();
        }
    }

    private void handleTimeChanged(long time) {
        timeSlider.setValue((int) time);

        String timeString = FormatUtils.formatTime(time);

        currentTimeLabel.setText(timeString);
    }

    private void handleToggleClip() {
        if (!clipStarted && !mediaPlayer.status().isPlaying()) return;

        clipStarted = !clipStarted;

        if (clipStarted) {
            clipStartAt = timeSlider.getValue();

            SwingUtilities.invokeLater(() -> {
                Hashtable labels = new Hashtable();
                JLabel arrowLabel = new JLabel("↑");
                arrowLabel.setForeground(Color.RED);
                labels.put((int) clipStartAt, arrowLabel);
                timeSlider.setLabelTable(labels);
            });
        } else {
            SwingUtilities.invokeLater(() -> timeSlider.setLabelTable(null));

            clipStopAt = timeSlider.getValue();
            final long length = timeSlider.getMaximum();

            if (clipStopAt > 0) {
                long seconds = (clipStopAt - clipStartAt) / 1_000L;

                if (seconds >= 1) {
                    clipTableModel.add(new ClipTask(context, currentFile, clipStartAt, clipStopAt, length));
                }
            }

            clipStartAt = 0;
            clipStopAt = 0;
        }

        setEnableStatusWhenClip(!clipStarted);

        videoPanel.setBorder(clipStarted ? new LineBorder(Color.RED) : null);
        clipButton.setText(String.format("Press to %s clip (space)", clipStarted ? "stop" : "start"));
        clipButton.setBorder(clipStarted ? new LineBorder(Color.RED) : null);

        requestFocus();
    }

    private void handleStartClipping() {
        final List<ClipTask> tasks = new ArrayList<>(
                clipTableModel.getTasks().stream()
                        .filter(task -> !task.getStatus().equals("done"))
                        .collect(Collectors.toList()));

        if (tasks.isEmpty()) return;

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        File selectedDirectory = null;

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = fileChooser.getSelectedFile();
        }

        if (selectedDirectory == null) return;

        final File directory = selectedDirectory;

        if (mediaPlayer.status().isPlaying()) mediaPlayer.controls().pause();

        final TaskManager taskManager = context.getSystem().getManager(TaskManager.class);

        final int secondsBefore = Integer.valueOf(beforeSpinner.getValue().toString());
        final int secondsAfter = Integer.valueOf(afterSpinner.getValue().toString());

        final int totalTasks = tasks.size();

        tasks.stream()
                .forEach(task -> {
                    task.setDirectory(directory);
                    task.setTimeSpan(secondsBefore, secondsAfter);
                    taskManager.add(task.getId(), task);
                });

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                setEnableStatusWhenClip(false);
                clipButton.setEnabled(false);
                playButton.setEnabled(false);
                timeSlider.setEnabled(false);
                clipProgressBar.setVisible(true);
            });

            try {
                final AtomicInteger finishedTasks = new AtomicInteger(0);

                while (!tasks.isEmpty()) {
                    final List<ClipTask> toRemove = tasks.stream()
                            .filter(task -> !task.isFresh() && !task.isRunning())
                            .collect(Collectors.toList());

                    toRemove.stream()
                            .forEach(task -> {
                                finishedTasks.incrementAndGet();
                                task.setStatus("done");
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        clipProgressBar.setValue(finishedTasks.get());
                                        clipTableModel.fireTableDataChanged();
                                    } catch (Exception ex) {
                                        // IGNORE
                                    }
                                });
                            });

                    tasks.removeAll(toRemove);

                    Thread.sleep(500L);
                }
            } catch (Exception ex) {
                context.error(ClipFrame.class, "", ex);
            }

            SwingUtilities.invokeLater(() -> {
                clipProgressBar.setVisible(false);
                setEnableStatusWhenClip(true);
                clipButton.setEnabled(true);
                playButton.setEnabled(true);
                timeSlider.setEnabled(true);
            });
        }).start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loadButton) {
            handleLoad();
        } else if(e.getSource() == playButton) {
            handlePlayPause();
        } else if (e.getSource() == clipButton) {
            handleToggleClip();
        } else if (e.getSource() == startClipButton) {
            handleStartClipping();
        } else if (e.getSource().equals("video_remove")) {
            final int row = Integer.valueOf(e.getActionCommand());
            handleVideoRemove(row);
        } else if (e.getSource().equals("clip_remove")) {
            final int row = Integer.valueOf(e.getActionCommand());
            handleClipRemove(row);
        } else if (e.getSource().equals("jump_to")) {
            final int row = Integer.valueOf(e.getActionCommand());
            handleJumpTo(row);
        }

        requestFocus();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        handleVideoSelection(videoTable.getSelectedRow());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getSource() == timeSlider) {
            timeSliding = false;
            if (clipStarted) {
                int currentTime = timeSlider.getValue();
                if (currentTime < clipStartAt) {
                    timeSlider.setValue((int) clipStartAt);
                }
            }

            mediaPlayer.controls().setTime(timeSlider.getValue());

            if (!mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().start();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getSource() == timeSlider) {
            timeSliding = true;
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.getSource() == timeSlider && timeSliding) {
            mediaPlayer.controls().setTime(timeSlider.getValue());
            //mediaPlayer.controls().start();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!mediaPlayer.status().isPlaying()) {
            handleTimeChanged(timeSlider.getValue());
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = KeyEvent.class.cast(event);

            if (keyEvent.paramString().contains("KEY_RELEASED")) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        keyEvent.consume();
                        if (clipButton.isEnabled()) handleToggleClip();
                        break;
                    case KeyEvent.VK_X:
                        keyEvent.consume();
                        if (!clipStarted) handleStartClipping();
                        break;
                    case KeyEvent.VK_RIGHT:
                        keyEvent.consume();
                        handleRightArrowKey(false);
                        break;
                    case KeyEvent.VK_LEFT:
                        keyEvent.consume();
                        handleLeftArrowKey(false);
                        break;
                }
            }

            if (keyEvent.paramString().contains("KEY_PRESSED")) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                        keyEvent.consume();
                        handleRightArrowKey(true);
                        break;
                    case KeyEvent.VK_LEFT:
                        keyEvent.consume();
                        handleLeftArrowKey(true);
                        break;
                }
            }
        }
    }

    @Override
    public void mediaDurationChanged(Media media, long newDuration) {
        timeSlider.setMaximum((int) newDuration);
    }

    @Override
    public void mediaFreed(Media media, MediaRef mediaFreed) {

    }

    @Override
    public void mediaMetaChanged(Media media, Meta metaType) {

    }

    @Override
    public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {

    }

    @Override
    public void mediaStateChanged(Media media, State newState) {

    }

    @Override
    public void mediaSubItemAdded(Media media, MediaRef newChild) {

    }

    @Override
    public void mediaSubItemTreeAdded(Media media, MediaRef item) {

    }

    @Override
    public void mediaThumbnailGenerated(Media media, Picture picture) {

    }

    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
        currentTimeLabel.setText("00:00:00");
    }

    @Override
    public void opening(MediaPlayer mediaPlayer) {

    }

    @Override
    public void buffering(MediaPlayer mediaPlayer, float newCache) {

    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {
        playButton.setText("Pause");
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
        playButton.setText("Play");
    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {

    }

    @Override
    public void forward(MediaPlayer mediaPlayer) {

    }

    @Override
    public void backward(MediaPlayer mediaPlayer) {

    }

    @Override
    public void finished(MediaPlayer mediaPlayer) {
        playButton.setText("Replay");
        handleTimeChanged(timeSlider.getMaximum());
        if (clipStarted) handleToggleClip();

        if (autoPlayCheckBox.isSelected()) {
            final int selectedRow = videoTable.getSelectedRow();
            final int rows = videoTableModel.getRowCount();

            if (selectedRow < rows - 1) {
                SwingUtilities.invokeLater(() -> {
                    videoTable.changeSelection(selectedRow + 1, 0, false, false);
                });
            }
        }
    }

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
        handleTimeChanged(newTime);
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {

    }

    @Override
    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {

    }

    @Override
    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {

    }

    @Override
    public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {

    }

    @Override
    public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {

    }

    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
        SwingUtilities.invokeLater(() -> {
            try {
                timeSlider.setMinimum(0);
                timeSlider.setMaximum((int) newLength);
                timeSlider.setLabelTable(null);
            } catch (Exception ex) {
                // IGNORE
            }
        });
    }

    @Override
    public void videoOutput(MediaPlayer mediaPlayer, int newCount) {

    }

    @Override
    public void scrambledChanged(MediaPlayer mediaPlayer, int newScrambled) {

    }

    @Override
    public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void corked(MediaPlayer mediaPlayer, boolean corked) {

    }

    @Override
    public void muted(MediaPlayer mediaPlayer, boolean muted) {

    }

    @Override
    public void volumeChanged(MediaPlayer mediaPlayer, float volume) {

    }

    @Override
    public void audioDeviceChanged(MediaPlayer mediaPlayer, String audioDevice) {

    }

    @Override
    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {

    }

    @Override
    public void error(MediaPlayer mediaPlayer) {

    }

    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer) {
        mediaPlayer.media().events().addMediaEventListener(this);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        vSpacer2 = new JPanel(null);
        label3 = new JLabel();
        hSpacer3 = new JPanel(null);
        label2 = new JLabel();
        beforeSpinner = new JSpinner();
        label4 = new JLabel();
        afterSpinner = new JSpinner();
        clipButton = new JButton();
        autoPlayCheckBox = new JCheckBox();
        loadButton = new JButton();
        videoTitleLabel = new JLabel();
        hSpacer1 = new JPanel(null);
        videoPanel = new JPanel();
        hSpacer4 = new JPanel(null);
        scrollPane1 = new JScrollPane();
        videoTable = new JTable();
        hSpacer2 = new JPanel(null);
        playButton = new JButton();
        timeSlider = new JSlider();
        currentTimeLabel = new JLabel();
        startClipButton = new JButton();
        clipProgressBar = new JProgressBar();
        scrollPane2 = new JScrollPane();
        clipTable = new JTable();
        vSpacer1 = new JPanel(null);

        //======== this ========
        setTitle("Clip Editor");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "7*(default, $lcgap), 300dlu:grow, 2*($lcgap, default), $lcgap, 200dlu, $lcgap, default",
            "3*(default, $lgap), 200dlu:grow, $lgap, default, $lgap, pref, $lgap, 107dlu, $lgap, default"));
        contentPane.add(vSpacer2, CC.xywh(15, 1, 7, 1));

        //---- label3 ----
        label3.setText("+ Seconds:");
        contentPane.add(label3, CC.xy(3, 3));
        contentPane.add(hSpacer3, CC.xy(5, 3));

        //---- label2 ----
        label2.setText("Before");
        contentPane.add(label2, CC.xy(7, 3));

        //---- beforeSpinner ----
        beforeSpinner.setModel(new SpinnerNumberModel(5, 0, 10, 1));
        beforeSpinner.setRequestFocusEnabled(false);
        contentPane.add(beforeSpinner, CC.xy(9, 3));

        //---- label4 ----
        label4.setText("After");
        contentPane.add(label4, CC.xy(11, 3));

        //---- afterSpinner ----
        afterSpinner.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        afterSpinner.setRequestFocusEnabled(false);
        contentPane.add(afterSpinner, CC.xy(13, 3));

        //---- clipButton ----
        clipButton.setText("Press to start clip (space)");
        clipButton.setEnabled(false);
        contentPane.add(clipButton, CC.xy(15, 3, CC.CENTER, CC.DEFAULT));

        //---- autoPlayCheckBox ----
        autoPlayCheckBox.setText("Auto Play");
        autoPlayCheckBox.setSelected(true);
        autoPlayCheckBox.setRequestFocusEnabled(false);
        contentPane.add(autoPlayCheckBox, CC.xy(17, 3));

        //---- loadButton ----
        loadButton.setText("Load");
        contentPane.add(loadButton, CC.xy(21, 3));
        contentPane.add(videoTitleLabel, CC.xywh(3, 5, 15, 1, CC.CENTER, CC.DEFAULT));
        contentPane.add(hSpacer1, CC.xywh(1, 7, 1, 7));

        //======== videoPanel ========
        {
            videoPanel.setLayout(new FormLayout(
                "default",
                "default"));
        }
        contentPane.add(videoPanel, CC.xywh(3, 7, 15, 1, CC.FILL, CC.FILL));
        contentPane.add(hSpacer4, CC.xy(19, 7));

        //======== scrollPane1 ========
        {

            //---- videoTable ----
            videoTable.setModel(new DefaultTableModel(
                new Object[][] {
                },
                new String[] {
                    "Video", " "
                }
            ) {
                boolean[] columnEditable = new boolean[] {
                    false, true
                };
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnEditable[columnIndex];
                }
            });
            {
                TableColumnModel cm = videoTable.getColumnModel();
                cm.getColumn(0).setMinWidth(200);
                cm.getColumn(0).setPreferredWidth(200);
                cm.getColumn(1).setMinWidth(80);
                cm.getColumn(1).setPreferredWidth(80);
            }
            scrollPane1.setViewportView(videoTable);
        }
        contentPane.add(scrollPane1, CC.xywh(21, 5, 1, 4, CC.FILL, CC.FILL));
        contentPane.add(hSpacer2, CC.xywh(23, 7, 1, 7));

        //---- playButton ----
        playButton.setText("Play");
        playButton.setEnabled(false);
        contentPane.add(playButton, CC.xy(3, 9));

        //---- timeSlider ----
        timeSlider.setValue(0);
        timeSlider.setEnabled(false);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintLabels(true);
        contentPane.add(timeSlider, CC.xywh(7, 9, 9, 1));

        //---- currentTimeLabel ----
        currentTimeLabel.setText("00:00:00");
        contentPane.add(currentTimeLabel, CC.xy(17, 9, CC.CENTER, CC.DEFAULT));

        //---- startClipButton ----
        startClipButton.setText("Clip All (x)");
        startClipButton.setEnabled(false);
        contentPane.add(startClipButton, CC.xy(21, 9));

        //---- clipProgressBar ----
        clipProgressBar.setVisible(false);
        contentPane.add(clipProgressBar, CC.xy(21, 11));

        //======== scrollPane2 ========
        {

            //---- clipTable ----
            clipTable.setModel(new DefaultTableModel(
                new Object[][] {
                },
                new String[] {
                    "Video", "From", "To", "Length", "Status", " ", " "
                }
            ) {
                boolean[] columnEditable = new boolean[] {
                    false, false, false, false, false, true, true
                };
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnEditable[columnIndex];
                }
            });
            {
                TableColumnModel cm = clipTable.getColumnModel();
                cm.getColumn(0).setMinWidth(200);
                cm.getColumn(0).setPreferredWidth(200);
                cm.getColumn(1).setMinWidth(80);
                cm.getColumn(1).setPreferredWidth(80);
                cm.getColumn(3).setMinWidth(80);
                cm.getColumn(3).setPreferredWidth(80);
                cm.getColumn(4).setMinWidth(100);
                cm.getColumn(4).setPreferredWidth(100);
                cm.getColumn(5).setMinWidth(80);
                cm.getColumn(5).setPreferredWidth(80);
                cm.getColumn(6).setMinWidth(80);
                cm.getColumn(6).setPreferredWidth(80);
            }
            scrollPane2.setViewportView(clipTable);
        }
        contentPane.add(scrollPane2, CC.xywh(3, 13, 19, 1, CC.FILL, CC.FILL));
        contentPane.add(vSpacer1, CC.xywh(15, 15, 7, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel vSpacer2;
    private JLabel label3;
    private JPanel hSpacer3;
    private JLabel label2;
    private JSpinner beforeSpinner;
    private JLabel label4;
    private JSpinner afterSpinner;
    private JButton clipButton;
    private JCheckBox autoPlayCheckBox;
    private JButton loadButton;
    private JLabel videoTitleLabel;
    private JPanel hSpacer1;
    private JPanel videoPanel;
    private JPanel hSpacer4;
    private JScrollPane scrollPane1;
    private JTable videoTable;
    private JPanel hSpacer2;
    private JButton playButton;
    private JSlider timeSlider;
    private JLabel currentTimeLabel;
    private JButton startClipButton;
    private JProgressBar clipProgressBar;
    private JScrollPane scrollPane2;
    private JTable clipTable;
    private JPanel vSpacer1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
