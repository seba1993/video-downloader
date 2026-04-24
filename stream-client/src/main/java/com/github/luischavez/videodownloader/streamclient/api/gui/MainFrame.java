package com.github.luischavez.videodownloader.streamclient.api.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.github.luischavez.videodownloader.streamclient.Main;
import com.github.luischavez.videodownloader.streamclient.api.Utils;
import com.github.luischavez.videodownloader.streamclient.api.gui.model.*;
import com.github.luischavez.videodownloader.streamclient.api.model.Video;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.*;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MainFrame extends JFrame
		implements ActionListener, ChangeListener,
		MouseListener, MouseMotionListener,
		ListSelectionListener, KeyListener,
		AWTEventListener, VideoPanel.SubtitleListener,
		MediaEventListener, MediaPlayerEventListener {

	public static final String APP_NAME = "Transcription Tool";
	public static final String VERSION = "v1.2.0";

	private static final String TITLE = String.format("%s %s", APP_NAME, VERSION);

	private static final Pattern TIME_PATTERN = Pattern.compile("(?<h>\\d+):(?<m>\\d+):(?<s>\\d+)");

	private VideoListModel pendingVideoListModel;
	private VideoListModel workingVideoListModel;

	private SubtitleTableModel subtitleTableModel;

	private Video selectedVideo;
	private List<Subtitle> subtitles;

	private VideoDialog videoDialog;

	private EmbeddedMediaPlayer mediaPlayer;

	private boolean timeSliding = false;

	private long lastTimeLeftArrowIncremented;
	private long lastTimeRightArrowIncremented;

	private long velocity;

	private Subtitle selectedSubtitle;

	private UndoManager undo = new UndoManager();

	private String lastInsertedShortcut;

	public MainFrame() {
		initComponents();
		initialize();
		initializePlayer();

		setTitle(TITLE);
	}

	private void initializePlayer() {
		List<String> vlcArgs = new ArrayList<>();

		vlcArgs.add("--no-snapshot-preview");
		vlcArgs.add("--quiet");

		MediaPlayerFactory playerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));

		mediaPlayer = playerFactory.mediaPlayers().newEmbeddedMediaPlayer();
		mediaPlayer.videoSurface().set(playerFactory.videoSurfaces().newVideoSurface(videoDialog.videoPanel.getSurface()));
		mediaPlayer.events().addMediaEventListener(this);
		mediaPlayer.events().addMediaPlayerEventListener(this);
	}

	private void initialize() {
		videoDialog = new VideoDialog(this);
		videoDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		videoDialog.setAlwaysOnTop(false);

		fastModeCheckBox.setSelected(false);
		fastModeCheckBox.setVisible(false);

		setFieldsEnableStatus(false);

		pendingVideoListModel = new VideoListModel("pending");
		workingVideoListModel = new VideoListModel("working");

		subtitleTableModel = new SubtitleTableModel(new ArrayList<>(), searchTextField);

		pendingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		workingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		subtitleTable.setModel(subtitleTableModel);
		subtitleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		subtitleTable.getTableHeader().setReorderingAllowed(false);

		TableColumnModel subtitleColumnModel = subtitleTable.getColumnModel();

		subtitleTable.setRowHeight(50);

		TableCenterCellRenderer centerCellRenderer = new TableCenterCellRenderer();

		for (int i = 0; i < SubtitleTableModel.COLUMNS.length; i++) {
			TableColumn column = subtitleColumnModel.getColumn(i);

			column.setCellRenderer(centerCellRenderer);

			column.setHeaderValue(SubtitleTableModel.COLUMNS[i]);
			column.setPreferredWidth(SubtitleTableModel.COLUMN_WIDTHS[i]);
			column.setWidth(SubtitleTableModel.COLUMN_WIDTHS[i]);
			column.setMinWidth(SubtitleTableModel.COLUMN_WIDTHS[i]);

			if (SubtitleTableModel.COLUMN_MAX_WIDTHS[i] > 0) {
				column.setMaxWidth(SubtitleTableModel.COLUMN_MAX_WIDTHS[i]);
			}
		}

		TimeSpinnerEditor timeSpinnerEditor = new TimeSpinnerEditor();
		timeSpinnerEditor.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent e) {
				Object value = timeSpinnerEditor.getCellEditorValue();

				int selectedRow = subtitleTable.getSelectedRow();
				int selectedColumn = subtitleTable.getSelectedColumn();
				Subtitle subtitle = subtitleTableModel.subtitles().get(selectedRow);

				if (subtitle != null) {
					if (selectedColumn == 0) {
						subtitle.setStartAt(Utils.parseDate(value.toString()));
					} else {
						subtitle.setEndAt(Utils.parseDate(value.toString()));
					}
				}

				SwingUtilities.invokeLater(() -> {
					subtitleTableModel.fireTableCellUpdated(selectedRow, selectedColumn);
					subtitleTable.changeSelection(selectedRow, 0, false, false);
				});
			}

			@Override
			public void editingCanceled(ChangeEvent e) {

			}
		});

		SubtitleTextEditor subtitleTextEditor = new SubtitleTextEditor();
		subtitleTextEditor.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent e) {
				Object value = subtitleTextEditor.getCellEditorValue();

				int selectedRow = subtitleTable.getSelectedRow();
				Subtitle subtitle = subtitleTableModel.subtitles().get(selectedRow);

				if (subtitle != null) {
					subtitle.setText(value.toString());
				}

				SwingUtilities.invokeLater(() -> {
					subtitleTableModel.fireTableCellUpdated(selectedRow, 2);
					subtitleTable.changeSelection(selectedRow, 0, false, false);
					setSubtitles();
				});
			}

			@Override
			public void editingCanceled(ChangeEvent e) {

			}
		});

		subtitleColumnModel.getColumn(0).setCellEditor(timeSpinnerEditor);
		subtitleColumnModel.getColumn(1).setCellEditor(timeSpinnerEditor);

		subtitleColumnModel.getColumn(2).setCellRenderer(new TableSubtitleCellRenderer());
		subtitleColumnModel.getColumn(2).setCellEditor(subtitleTextEditor);

		/*new ButtonColumn(subtitleTable, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = Integer.valueOf(e.getActionCommand());

				subtitleTableModel.remove(index);
			}
		}, 3);*/

		pendingList.setModel(pendingVideoListModel);
		workingList.setModel(workingVideoListModel);

		EditorKit editorKit = new StyledEditorKit();

		transcriptionTextPane.setEditorKit(editorKit);
		transcriptionTextPane.setDocument(editorKit.createDefaultDocument());

		Document transcriptionDocument = transcriptionTextPane.getDocument();

		transcriptionDocument.addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent evt) {
				undo.addEdit(evt.getEdit());
			}
		});

		transcriptionTextPane.getActionMap().put("Undo", new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canUndo()) {
						undo.undo();
					}
				} catch (CannotUndoException e) {
				}
			}
		});

		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

		pendingList.addListSelectionListener(this);
		workingList.addListSelectionListener(this);

		reloadButton.addActionListener(this);
		statusButton.addActionListener(this);
		saveButton.addActionListener(this);

		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
		videoDialog.videoPanel.setSubtitleListener(this);

		transcriptionTextPane.addKeyListener(this);

		searchTextField.addKeyListener(this);

		playButton.addActionListener(this);

		timeSlider.addMouseListener(this);
		timeSlider.addMouseMotionListener(this);
		timeSlider.addChangeListener(this);

		timeSlider.setValue(0);
		timeSlider.setPaintTicks(true);
		timeSlider.setPaintLabels(true);
		timeSlider.setMajorTickSpacing(1_000);
		timeSlider.setLabelTable(null);

		subtitleTable.addMouseListener(this);

		hideVideoCheckBox.addActionListener(this);

		fontSpinner.addChangeListener(this);

		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), "doNothing");
		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), "doNothing");
		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), "doNothing");
		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke("control C"), "doNothing");
		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke("control V"), "doNothing");
		transcriptionTextPane.getInputMap().put(KeyStroke.getKeyStroke("control X"), "doNothing");
		transcriptionTextPane.getActionMap().put("doNothing", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == reloadButton) {
			SwingUtilities.invokeLater(() -> {
				pendingVideoListModel.reload();
				workingVideoListModel.reload();
			});
		} else if (e.getSource() == statusButton) {
			Video video = Main.setStatus(selectedVideo, statusButton.getActionCommand());

			if (video != null) {
				if (video.getStatus().equals("finished")) video = null;

				reloadButton.doClick();
				handleVideoSelection(video);
			}
		} else if (e.getSource() == playButton) {
			switch (playButton.getText()) {
				case "Play":
					mediaPlayer.controls().play();
					break;
				case "Replay":
					mediaPlayer.controls().setPosition(0);
					mediaPlayer.controls().play();
					break;
				default:
					mediaPlayer.controls().pause();
			}
		} else if (e.getSource() == saveButton) {
			int option = JOptionPane.showConfirmDialog(this, "Are you sure?", "continue to save the data?", JOptionPane.OK_CANCEL_OPTION);

			if (option == JOptionPane.OK_OPTION) {
				String srt = Utils.parseSubtitle(subtitles);
				String transcription = transcriptionTextPane.getText();

				Video savedVideo = Main.save(selectedVideo, srt, transcription);

				if (savedVideo != null) {
					JOptionPane.showMessageDialog(this, "SUCCESS!", "saved data.", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, "ERROR!", "can't save data.", JOptionPane.ERROR_MESSAGE);
				}
			}
		} else if (e.getSource() == hideVideoCheckBox) {
			boolean hide = hideVideoCheckBox.isSelected();

			if (hide) {
				if (videoDialog.isVisible()) videoDialog.setVisible(false);
			} else {
				if (!videoDialog.isVisible()) videoDialog.setVisible(true);
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == timeSlider) {
			if (mediaPlayer != null && mediaPlayer.media().isValid()) {
				int time = timeSlider.getValue();

				handleTimeChanged(time);
			}
		} else if (e.getSource() == fontSpinner) {
			int fontSize = (int) fontSpinner.getValue();

			transcriptionTextPane.setFont(getFont(fontSize));
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == subtitleTable && e.getClickCount() == 1) {
			int row = subtitleTable.rowAtPoint(e.getPoint());

			try {
				Subtitle subtitle = subtitleTableModel.subtitles().get(row);

				if (subtitle != null) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(subtitle.getStartAt());

					long time = 0;

					int hours = calendar.get(Calendar.HOUR_OF_DAY);
					int minutes = calendar.get(Calendar.MINUTE);
					int seconds = calendar.get(Calendar.SECOND);

					minutes += hours * 60;
					seconds += minutes * 60;

					time += seconds * 1_000L;

					mediaPlayer.controls().setTime(time);
					handleTimeChanged(time);
				}
			} catch (Exception ex) {
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.getSource() == timeSlider && timeSliding) {
			mediaPlayer.controls().setTime(timeSlider.getValue());
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

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
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == timeSlider) {
			timeSliding = false;

			mediaPlayer.controls().setTime(timeSlider.getValue());

			if (!mediaPlayer.status().isPlaying()) {
				mediaPlayer.controls().start();
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int index = e.getFirstIndex();

		if (e.getSource() == pendingList) {
			handleVideoSelection(pendingVideoListModel.video(index));
		} else if (e.getSource() == workingList) {
			handleVideoSelection(workingVideoListModel.video(index));
		}

		pendingList.removeListSelectionListener(this);
		workingList.removeListSelectionListener(this);

		pendingList.clearSelection();
		workingList.clearSelection();

		pendingList.addListSelectionListener(this);
		workingList.addListSelectionListener(this);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getSource() == transcriptionTextPane) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				e.consume();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getSource() == searchTextField) {
			subtitleTableModel.fireTableDataChanged();
		}

		if (e.getSource() == transcriptionTextPane) {
			if (e.isControlDown() && e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_8) {
				insertShortcut(e.getKeyCode());
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				e.consume();
				if (isFastModeShortcut()) {
					handleShortcutInsertion(getNextShortcut());
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void eventDispatched(AWTEvent event) {
		if (event instanceof KeyEvent) {
			Component focusOwner = getFocusOwner();

			KeyEvent keyEvent = KeyEvent.class.cast(event);

			if (keyEvent.getKeyCode() == KeyEvent.VK_TAB) {
				keyEvent.consume();

				if (keyEvent.paramString().contains("KEY_RELEASED")) {
					if (selectedVideo != null) {
						if (mediaPlayer.status().isPlaying()) {
							mediaPlayer.controls().pause();
						} else {
							mediaPlayer.controls().play();
						}
					}
				}

				return;
			}

			if (!(focusOwner instanceof JTextComponent)) {
				if (keyEvent.paramString().contains("KEY_RELEASED")) {
					switch (keyEvent.getKeyCode()) {
						case KeyEvent.VK_RIGHT:
							keyEvent.consume();
							handleRightArrowKey(false);
							return;
						case KeyEvent.VK_LEFT:
							keyEvent.consume();
							handleLeftArrowKey(false);
							return;
					}
				}

				if (keyEvent.paramString().contains("KEY_PRESSED")) {
					switch (keyEvent.getKeyCode()) {
						case KeyEvent.VK_RIGHT:
							keyEvent.consume();
							handleRightArrowKey(true);
							return;
						case KeyEvent.VK_LEFT:
							keyEvent.consume();
							handleLeftArrowKey(true);
							return;
					}
				}

				if (keyEvent.getKeyCode() == KeyEvent.VK_I) {
					if (!fastModeCheckBox.isSelected() || videoDialog.videoPanel.getSubtitleArea().isVisible() || (focusOwner != null && focusOwner instanceof JTextComponent)) return;

					videoDialog.videoPanel.getSubtitleArea().setVisible(true);
					videoDialog.videoPanel.getSubtitleArea().grabFocus();
				}
			}
		}
 	}

	@Override
	public void onSubtitleFinished(String subtitle) {
		System.out.println(subtitle);
	}

	private void handleArrowKey(boolean pressed, boolean right) {
		if (selectedVideo == null) return;

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

	private void handleTimeChanged(long time) {
		timeSlider.setValue((int) time);
		currentTimeLabel.setText(formatTime(time));
	}

	private void handleRightArrowKey(boolean pressed) {
		handleArrowKey(pressed, true);
	}

	private void handleLeftArrowKey(boolean pressed) {
		handleArrowKey(pressed, false);
	}

	private int getLastTranscriptionBreakPoint() {
		char[] chars = transcriptionTextPane.getText().toCharArray();

		for (int index = chars.length - 1; index >= 0; index--) {
			if (chars[index] == '\n') {
				return index;
			}
		}

		return 0;
	}

	private boolean isSpaceChar(char c) {
		return Character.isSpaceChar(c) || c == '\n';
	}

	private void handleShortcutInsertion(String shortcutText) {
		lastInsertedShortcut = shortcutText;

		int caretPosition = transcriptionTextPane.getCaretPosition();
		int lastBreakPoint = getLastTranscriptionBreakPoint();

		String transcriptionText = transcriptionTextPane.getText();

		String target = transcriptionText.substring(lastBreakPoint, caretPosition);

		String newText = String.format("\n%s:\n%s\n\n", shortcutText, target.trim());

		try {
			transcriptionTextPane.getDocument().remove(lastBreakPoint, target.length());
			transcriptionTextPane.getDocument().insertString(lastBreakPoint, newText, new SimpleAttributeSet());
			transcriptionTextPane.setCaretPosition(lastBreakPoint + newText.length());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void scrollToSubtitle(int row) {
		try {
			subtitleTable.scrollRectToVisible(new Rectangle(subtitleTable.getCellRect(row, 0, true)));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void selectSubtitle() {
		int time = timeSlider.getValue();
		String timeFormatted = formatTime(time);

		Matcher matcher = TIME_PATTERN.matcher(timeFormatted);

		if (matcher.find()) {
			int hours = Integer.valueOf(matcher.group("h"));
			int minutes = Integer.valueOf(matcher.group("m"));
			int seconds = Integer.valueOf(matcher.group("s"));

			Calendar calendar = Calendar.getInstance();

			selectedSubtitle = subtitles.stream()
					.sorted(Comparator.comparing(Subtitle::getStartAt))
					.filter(subtitle -> {
						calendar.setTime(subtitle.getStartAt());
						calendar.set(Calendar.HOUR_OF_DAY, hours);
						calendar.set(Calendar.MINUTE, minutes);
						calendar.set(Calendar.SECOND, seconds);

						Date date = calendar.getTime();

						return date.after(subtitle.getStartAt()) && date.before(subtitle.getEndAt());
					})
					.findFirst()
					.orElse(null);

			if (selectedSubtitle != null) {
				int subtitleIndex = subtitleTableModel.indexOf(selectedSubtitle);
				subtitleTable.setRowSelectionInterval(subtitleIndex, subtitleIndex);

				scrollToSubtitle(subtitleIndex);
			}
		}
	}

	private boolean isFastModeShortcut() {
		int shortcutCount = 0;

		for (Component component : getContentPane().getComponents()) {
			String componentName = component.getName();

			if (componentName != null && componentName.startsWith("shortcut")) {
				String shortcutText = ((JTextField) component).getText();

				if (!shortcutText.trim().isEmpty()) shortcutCount++;
			}
		}

		return shortcutCount == 2;
	}

	private int indexOfShortcut(String shortcut) {
		String[] shortcuts = new String[8];

		for (Component component : getContentPane().getComponents()) {
			String componentName = component.getName();

			if (componentName != null && componentName.startsWith("shortcut")) {
				int shortcutNumber = Integer.valueOf(componentName.replace("shortcut", ""));
				String shortcutText = ((JTextField) component).getText().trim();

				shortcuts[shortcutNumber - 1] = shortcutText;
			}
		}

		for (int i = 0; i < shortcuts.length; i++) {
			if (shortcuts[i].equals(shortcut)) return i;
		}

		return -1;
	}

	private String getNextShortcut() {
		String[] shortcuts = new String[8];

		for (Component component : getContentPane().getComponents()) {
			String componentName = component.getName();

			if (componentName != null && componentName.startsWith("shortcut")) {
				int shortcutNumber = Integer.valueOf(componentName.replace("shortcut", ""));
				String shortcutText = ((JTextField) component).getText().trim();

				shortcuts[shortcutNumber - 1] = shortcutText;
			}
		}

		if (lastInsertedShortcut == null) {
			for (String shortcutText : shortcuts) {
				if (!shortcutText.isEmpty()) return shortcutText;
			}
		} else {
			int indexOfLastShortcut = indexOfShortcut(lastInsertedShortcut);

			if (indexOfLastShortcut > -1) {
				if (indexOfLastShortcut == shortcuts.length - 1) indexOfLastShortcut = -1;

				for (int i = indexOfLastShortcut + 1; i < shortcuts.length; i++) {
					String shortcutText = shortcuts[i];

					if (!shortcutText.isEmpty()) return shortcutText;
				}

				for (int i = 0; i < shortcuts.length; i++) {
					String shortcutText = shortcuts[i];

					if (!shortcutText.isEmpty()) return shortcutText;
				}
			}
		}

		return "";
	}

 	private void insertShortcut(int keyCode) {
		int firstKeyCodeIndex = KeyEvent.VK_1;

		int realKey = Math.abs(firstKeyCodeIndex - keyCode) + 1;

		if (realKey < 1 || realKey > 8) return;

		Component[] components = getContentPane().getComponents();

		for (Component component : components) {
			String componentName = component.getName();

			if (componentName != null && componentName.equals("shortcut" + realKey)) {
				String shortcutText = ((JTextField) component).getText();

				Component focusOwner = getFocusOwner();

				if (focusOwner instanceof JTextComponent) {
					handleShortcutInsertion(shortcutText);
				}
			}
		}
	}

	private String getNextStatus() {
		if (selectedVideo != null) {
			switch (selectedVideo.getStatus()) {
				case "pending":
					return "working";
				case "working":
					return "finished";
			}
		}

		return "";
	}

	private void setFieldsEnableStatus(boolean enabled) {
		statusButton.setEnabled(enabled);
		playButton.setEnabled(enabled);
		saveButton.setEnabled(enabled);
		subtitleTable.setEnabled(enabled);
		fastModeCheckBox.setEnabled(false);
		transcriptionTextPane.setEnabled(enabled);
		searchTextField.setEnabled(enabled);
	}

	private void clearFields() {
		videoDialog.videoTitleLabel.setText("");
		statusButton.setText("");
		statusButton.setActionCommand("");
		subtitleTableModel.clear();
		subtitles = null;
		undo.discardAllEdits();
		lastInsertedShortcut = null;
	}

	private void setSubtitles() {
		try {
			Path subFilePath = Files.createTempFile("sub_", new Random().nextLong() + "");
			File subFile = subFilePath.toFile();

			Files.write(subFilePath, Utils.parseSubtitle(subtitles).getBytes());

			mediaPlayer.subpictures().setSubTitleFile(subFile);
		} catch (Exception ex) {
			ex.printStackTrace();
			// IGNORE
		}
	}

	private Font getFont(int size) {
		String fontName = transcriptionTextPane.getFont().getFontName();

		return new Font(fontName, Font.PLAIN, size);
	}

	private void handleVideoSelection(Video video) {
		if (selectedVideo != null && selectedVideo.getId() == video.getId()) return;

		if (selectedVideo != null) {
			int option = JOptionPane.showConfirmDialog(this, "are you sure you already save your work?", "Attention!", JOptionPane.OK_CANCEL_OPTION);

			if (option != JOptionPane.OK_OPTION) {
				return;
			}
		}

		selectedVideo = video;
		setFieldsEnableStatus(video != null);
		clearFields();
		if (video == null) return;

		videoDialog.setTitle(video.getName());
		videoDialog.videoTitleLabel.setText(video.getName());
		statusButton.setText(getNextStatus());
		statusButton.setActionCommand(getNextStatus());

		subtitles = Utils.parseSubtitle(video.getSubtitle());

		if (video.getTranscription().isEmpty()) {
			String subtitlesText = subtitles.stream().map(s -> s.getText()).collect(Collectors.joining(" "));
			transcriptionTextPane.setText(subtitlesText.toLowerCase().replaceAll("\\s+", " "));
		} else {
			transcriptionTextPane.setText(video.getTranscription().toLowerCase().replaceAll("\\s+", " "));
		}

		transcriptionTextPane.setCaretPosition(0);

		subtitleTableModel.setSubtitles(subtitles);

		//boolean hideVideo = hideVideoCheckBox.isSelected();

		hideVideoCheckBox.setSelected(false);
		if (!videoDialog.isVisible() && !hideVideoCheckBox.isSelected()) videoDialog.setVisible(true);

		mediaPlayer.media().play(video.getLink());

		//hideVideoCheckBox.setSelected(hideVideo);
		//if (videoDialog.isVisible() && hideVideoCheckBox.isSelected()) videoDialog.setVisible(false);

		int fontSize = (int) fontSpinner.getValue();
		transcriptionTextPane.setFont(getFont(fontSize));

		setSubtitles();
	}

	private static String formatTime(long millis) {
		long seconds = millis / 1_000;
		long minutes = seconds / 60;
		seconds %= 60;
		long hours = minutes / 60;
		minutes %= 60;

		return String.format("%s:%s:%s",
				hours < 10 ? "0" + hours : hours,
				minutes < 10 ? "0" + minutes : minutes,
				seconds < 10 ? "0" + seconds : seconds);
	}

	@Override
	public void mediaMetaChanged(Media media, Meta metaType) {

	}

	@Override
	public void mediaSubItemAdded(Media media, MediaRef newChild) {

	}

	@Override
	public void mediaDurationChanged(Media media, long newDuration) {

	}

	@Override
	public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {

	}

	@Override
	public void mediaFreed(Media media, MediaRef mediaFreed) {

	}

	@Override
	public void mediaStateChanged(Media media, State newState) {

	}

	@Override
	public void mediaSubItemTreeAdded(Media media, MediaRef item) {

	}

	@Override
	public void mediaThumbnailGenerated(Media media, Picture picture) {

	}

	@Override
	public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {

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
	}

	@Override
	public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
		currentTimeLabel.setText(formatTime(newTime));
		SwingUtilities.invokeLater(() -> {
			timeSlider.setValue((int) newTime);
			timeSlider.setLabelTable(null);
			selectSubtitle();
		});
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
		timeLabel.setText(formatTime(newLength));
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

	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		vSpacer1 = new JPanel(null);
		statusButton = new JButton();
		hideVideoCheckBox = new JCheckBox();
		panel1 = new JPanel();
		playButton = new JButton();
		currentTimeLabel = new JLabel();
		timeSlider = new JSlider();
		timeLabel = new JLabel();
		hSpacer1 = new JPanel(null);
		videoListTabbedPane = new JTabbedPane();
		pendingScrollPane = new JScrollPane();
		pendingList = new JList();
		workingScrollPane = new JScrollPane();
		workingList = new JList();
		hSpacer2 = new JPanel(null);
		saveButton = new JButton();
		fastModeCheckBox = new JCheckBox();
		vSpacer3 = new JPanel(null);
		reloadButton = new JButton();
		tabbedPane1 = new JTabbedPane();
		scrollPane1 = new JScrollPane();
		transcriptionTextPane = new JTextPane();
		panel2 = new JPanel();
		label1 = new JLabel();
		searchTextField = new JTextField();
		subtitleScrollPane = new JScrollPane();
		subtitleTable = new JTable();
		shortcutLabel = new JLabel();
		shortcut1TextField = new JTextField();
		shortcut2TextField = new JTextField();
		shortcut3TetField = new JTextField();
		shortcut4TextField = new JTextField();
		shortcut5TextField = new JTextField();
		shortcut6TextField = new JTextField();
		shortcut7TextField = new JTextField();
		shortcut8TextField = new JTextField();
		hSpacer3 = new JPanel(null);
		label2 = new JLabel();
		fontSpinner = new JSpinner();
		vSpacer2 = new JPanel(null);

		//======== this ========
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("Stream Client");
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"default, $lcgap, 90dlu, $lcgap, default, $lcgap, 58dlu, 8*($lcgap, 50dlu), $lcgap, default, $lcgap, 52dlu, $lcgap, right:31dlu:grow, $lcgap, default",
			"7*(default, $lgap), 165dlu:grow, $lgap, default:grow, 2*($lgap, default)"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 28, 1));

		//---- statusButton ----
		statusButton.setText("Mark as working");
		contentPane.add(statusButton, CC.xy(3, 3));

		//---- hideVideoCheckBox ----
		hideVideoCheckBox.setText("Hide video");
		contentPane.add(hideVideoCheckBox, CC.xy(7, 3));

		//======== panel1 ========
		{
			panel1.setLayout(new FormLayout(
				"2*(default, $lcgap), default:grow, $lcgap, default",
				"default"));

			//---- playButton ----
			playButton.setText("Play");
			panel1.add(playButton, CC.xy(1, 1));

			//---- currentTimeLabel ----
			currentTimeLabel.setText("00:00:00");
			panel1.add(currentTimeLabel, CC.xy(3, 1));
			panel1.add(timeSlider, CC.xy(5, 1));

			//---- timeLabel ----
			timeLabel.setText("00:00:00");
			panel1.add(timeLabel, CC.xy(7, 1));
		}
		contentPane.add(panel1, CC.xywh(9, 3, 21, 1));
		contentPane.add(hSpacer1, CC.xywh(1, 7, 1, 9));

		//======== videoListTabbedPane ========
		{

			//======== pendingScrollPane ========
			{

				//---- pendingList ----
				pendingList.setPreferredSize(new Dimension(39, 500));
				pendingScrollPane.setViewportView(pendingList);
			}
			videoListTabbedPane.addTab("Pending", pendingScrollPane);

			//======== workingScrollPane ========
			{
				workingScrollPane.setViewportView(workingList);
			}
			videoListTabbedPane.addTab("Working", workingScrollPane);
		}
		contentPane.add(videoListTabbedPane, CC.xywh(3, 13, 1, 5, CC.FILL, CC.FILL));
		contentPane.add(hSpacer2, CC.xywh(31, 7, 1, 9));

		//---- saveButton ----
		saveButton.setText("Save");
		contentPane.add(saveButton, CC.xy(3, 5));

		//---- fastModeCheckBox ----
		fastModeCheckBox.setText("Fast mode (Press I)");
		contentPane.add(fastModeCheckBox, CC.xy(3, 7, CC.CENTER, CC.CENTER));
		contentPane.add(vSpacer3, CC.xy(3, 9));

		//---- reloadButton ----
		reloadButton.setText("Reload");
		contentPane.add(reloadButton, CC.xy(3, 11));

		//======== tabbedPane1 ========
		{

			//======== scrollPane1 ========
			{
				scrollPane1.setViewportView(transcriptionTextPane);
			}
			tabbedPane1.addTab("Transcription", scrollPane1);

			//======== panel2 ========
			{
				panel2.setLayout(new FormLayout(
					"26dlu, $lcgap, default, $lcgap, default:grow, $lcgap, default",
					"default, $lgap, default:grow, $lgap, default"));

				//---- label1 ----
				label1.setText("Buscar:");
				panel2.add(label1, CC.xywh(1, 1, 3, 1));
				panel2.add(searchTextField, CC.xy(5, 1));

				//======== subtitleScrollPane ========
				{

					//---- subtitleTable ----
					subtitleTable.setModel(new DefaultTableModel(
						new Object[][] {
						},
						new String[] {
							"#", "Start", "End", "Text", "Delete"
						}
					) {
						boolean[] columnEditable = new boolean[] {
							false, true, true, true, true
						};
						@Override
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return columnEditable[columnIndex];
						}
					});
					subtitleScrollPane.setViewportView(subtitleTable);
				}
				panel2.add(subtitleScrollPane, CC.xywh(1, 3, 7, 3));
			}
			tabbedPane1.addTab("SRT", panel2);
		}
		contentPane.add(tabbedPane1, CC.xywh(7, 5, 24, 13));

		//---- shortcutLabel ----
		shortcutLabel.setText("Shortcuts");
		contentPane.add(shortcutLabel, CC.xy(7, 19, CC.CENTER, CC.DEFAULT));

		//---- shortcut1TextField ----
		shortcut1TextField.setText("Interlocutor 1");
		shortcut1TextField.setName("shortcut1");
		contentPane.add(shortcut1TextField, CC.xy(9, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut2TextField ----
		shortcut2TextField.setText("Interlocutor 2");
		shortcut2TextField.setName("shortcut2");
		contentPane.add(shortcut2TextField, CC.xy(11, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut3TetField ----
		shortcut3TetField.setName("shortcut3");
		contentPane.add(shortcut3TetField, CC.xy(13, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut4TextField ----
		shortcut4TextField.setName("shortcut4");
		contentPane.add(shortcut4TextField, CC.xy(15, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut5TextField ----
		shortcut5TextField.setName("shortcut5");
		contentPane.add(shortcut5TextField, CC.xy(17, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut6TextField ----
		shortcut6TextField.setName("shortcut6");
		contentPane.add(shortcut6TextField, CC.xy(19, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut7TextField ----
		shortcut7TextField.setName("shortcut7");
		contentPane.add(shortcut7TextField, CC.xy(21, 19, CC.FILL, CC.DEFAULT));

		//---- shortcut8TextField ----
		shortcut8TextField.setName("shortcut8");
		contentPane.add(shortcut8TextField, CC.xy(23, 19, CC.FILL, CC.DEFAULT));
		contentPane.add(hSpacer3, CC.xy(25, 19));

		//---- label2 ----
		label2.setText("Font size");
		contentPane.add(label2, CC.xy(27, 19, CC.RIGHT, CC.DEFAULT));

		//---- fontSpinner ----
		fontSpinner.setModel(new SpinnerNumberModel(13, 8, 28, 1));
		contentPane.add(fontSpinner, CC.xy(29, 19, CC.LEFT, CC.DEFAULT));
		contentPane.add(vSpacer2, CC.xywh(3, 21, 28, 1));
		setSize(1300, 775);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	private JPanel vSpacer1;
	private JButton statusButton;
	private JCheckBox hideVideoCheckBox;
	private JPanel panel1;
	private JButton playButton;
	private JLabel currentTimeLabel;
	private JSlider timeSlider;
	private JLabel timeLabel;
	private JPanel hSpacer1;
	private JTabbedPane videoListTabbedPane;
	private JScrollPane pendingScrollPane;
	private JList pendingList;
	private JScrollPane workingScrollPane;
	private JList workingList;
	private JPanel hSpacer2;
	private JButton saveButton;
	private JCheckBox fastModeCheckBox;
	private JPanel vSpacer3;
	private JButton reloadButton;
	private JTabbedPane tabbedPane1;
	private JScrollPane scrollPane1;
	private JTextPane transcriptionTextPane;
	private JPanel panel2;
	private JLabel label1;
	private JTextField searchTextField;
	private JScrollPane subtitleScrollPane;
	private JTable subtitleTable;
	private JLabel shortcutLabel;
	private JTextField shortcut1TextField;
	private JTextField shortcut2TextField;
	private JTextField shortcut3TetField;
	private JTextField shortcut4TextField;
	private JTextField shortcut5TextField;
	private JTextField shortcut6TextField;
	private JTextField shortcut7TextField;
	private JTextField shortcut8TextField;
	private JPanel hSpacer3;
	private JLabel label2;
	private JSpinner fontSpinner;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
