package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.*;
import com.github.luischavez.videodownloader.app.gui.model.ScheduleFolderModel;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.app.task.ThreadedConcatenationTask;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResult;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResults;
import com.github.luischavez.videodownloader.schedule.KeepRunningScheduleTask;
import com.github.luischavez.videodownloader.schedule.Schedule;
import com.github.luischavez.videodownloader.schedule.ScheduleManager;
import com.github.luischavez.videodownloader.task.TaskManager;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.github.luischavez.videodownloader.util.PlatformUtils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Luis Chavez
 */
public class ScheduleConcatenationFrame extends JFrame implements ActionListener, ChangeListener, WindowListener {

	public static final String VERSION = "v2.0.0";
	public static final String JAVA_VERSION = System.getProperty("java.version");
	public static final String TITLE = String.format("Schedule Concatenation Tool %s [Runtime %s]", VERSION, JAVA_VERSION);

	private final Context context;

	private LoadingDialog loadingDialog;
	private ScheduleConcatConfigurationDialog scheduleConcatConfigurationDialog;

	private MultipleConcatenationTask multipleConcatenationTask;

	private TrayIcon trayIcon;

	public ScheduleConcatenationFrame(Context context) {
		initComponents();

		this.context = context;

		initialize();
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

	private void deleteFolder(String folder) {
		ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

		ScheduleFolderConfiguration configuration = configurationManager.list(ScheduleFolderConfiguration.class).stream()
				.filter(s -> s.getFolder().equals(folder))
				.findFirst()
				.orElse(null);

		if (configuration == null) return;

		configurationManager.remove(configuration);
	}

	private final void initialize() {
		addWindowListener(this);

		setTitle(TITLE);
		setAutoRequestFocus(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);
		setVisible(true);

		loadingDialog = new LoadingDialog(this);
		scheduleConcatConfigurationDialog = new ScheduleConcatConfigurationDialog(this, context);

		addButton.addActionListener(this);

		autosubTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String autosubPath = autosubTextField.getText();

				PathConfiguration pathConfiguration = new PathConfiguration();
				pathConfiguration.setAutosubPath(autosubPath);

				context.getSystem().getManager(ConfigurationManager.class).add(pathConfiguration);
			}
		});

		dayComboBox.addActionListener(this);
		hourSpinner.addChangeListener(this);
		minuteSpinner.addChangeListener(this);

		startButton.addActionListener(this);

		scheduleConcatConfigurationDialog.saveButton.addActionListener(this);

		readConfiguration();

		ScheduleFolderModel scheduleFolderModel = new ScheduleFolderModel(context);
		folderTable.setModel(scheduleFolderModel);

		new ButtonColumn(folderTable, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String folder = folderTable.getValueAt(Integer.valueOf(e.getActionCommand()), 0).toString();

				SwingUtilities.invokeLater(() -> scheduleConcatConfigurationDialog.open(folder));
			}
		}, 3);

		new ButtonColumn(folderTable, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String folder = folderTable.getValueAt(Integer.valueOf(e.getActionCommand()), 0).toString();

				int option = JOptionPane.showConfirmDialog(ScheduleConcatenationFrame.this, "are you sure?", String.format("Delete %s folder", folder), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

				if (option == JOptionPane.YES_OPTION) {
					deleteFolder(folder);
				}
			}
		}, 4);
	}

	private ThreadedConcatenationTask buildConcatenationTask(File sourceFile, String extension, File destinationFile, String language, boolean concatenate, boolean sub, boolean extractAudio) {
		ThreadedConcatenationTask.ConcatenationTaskBuilder builder
				= new ThreadedConcatenationTask.ConcatenationTaskBuilder(context);

		if (concatenate) builder.concatenate();
		if (sub) builder.sub();
		if (extractAudio) builder.extractAudio();

		List<File> files = new ArrayList<>();

		if (sourceFile.isFile()) {
			files.add(sourceFile);
		} else {
			files.addAll(Arrays.asList(sourceFile.listFiles((dir, name) -> name.toUpperCase().endsWith(extension.toUpperCase()))));
			files = files.stream()
					.filter(file -> file.length() >= 2048)
					.collect(Collectors.toList());
		}

		builder
				.sources(files.toArray(new File[0]))
				.destination(destinationFile)
				.language(language)
				.extension(extension);

		builder
				.onInput(this::log)
				.onStart(this::showLoading)
				.onStop(this::hideLoading);

		return builder.build();
	}

	private boolean shouldRun() {
		return startButton.getText().equals("Stop");
	}

	private void updateSchedule() {
		final ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);
		final ScheduleManager scheduleManager = context.getSystem().getManager(ScheduleManager.class);
		final TaskManager taskManager = context.getSystem().getManager(TaskManager.class);

		ScheduleConcatConfiguration configuration = configurationManager.get(ScheduleConcatConfiguration.class);

		ScheduleManager.ScheduleEntry scheduleEntry = scheduleManager.get(1);
		scheduleManager.remove(1);

		if (scheduleEntry != null) {
			try {
				scheduleEntry.getTask().disable();
			} catch (Exception ex) {
				log(ex.getMessage());
			}
		}

		if (!shouldRun()) return;

		scheduleManager.add(1, configuration.getSchedule(), new KeepRunningScheduleTask(context) {

			@Override
			protected void doTask() throws Exception {
				if (isRunning()) return;

				final String extension = "mkv";
				final List<ScheduleFolderConfiguration> configurations = configurationManager.list(ScheduleFolderConfiguration.class);

				Queue<ThreadedConcatenationTask> threadedConcatenationTasks = new ArrayDeque<>();

				for (ScheduleFolderConfiguration configuration : configurations) {
					File[] mediaFolderFiles = new File(configuration.getFolder()).listFiles((dir, name) -> {
						File file = new File(dir, name);

						if (!file.isDirectory()) return false;

						File[] finishedFlagFiles = file.listFiles((dir1, name1) -> name1.equals(file.getName() + "." + extension));
						File[] mediaFiles = file.listFiles((dir1, name1) -> name1.toUpperCase().endsWith(extension.toUpperCase()));

						if (mediaFiles == null || mediaFiles.length == 0) return false;

						return finishedFlagFiles == null || finishedFlagFiles.length == 0;
					});

					if (mediaFolderFiles != null && mediaFolderFiles.length > 0) {
						for (int i = 0; i < mediaFolderFiles.length; i++) {
							File mediaFolderFile = mediaFolderFiles[i];
							String parent = mediaFolderFile.getParentFile().getName();
							File calculatedDestinationFile = new File(mediaFolderFile.getParent(), String.format("%s-%s", parent, mediaFolderFile.getName()) + "." + extension);

							if (calculatedDestinationFile.exists()) {
								continue;
							}

							threadedConcatenationTasks.add(
									buildConcatenationTask(
											mediaFolderFile, extension,
											calculatedDestinationFile,
											configuration.getLanguage(),
											true, configuration.isSub(), true));
						}
					}
				}

				multipleConcatenationTask = new MultipleConcatenationTask(taskManager, threadedConcatenationTasks);
				multipleConcatenationTask.start();
			}

			@Override
			protected void doDisable() throws Exception {
				// NEVER DISABLED
			}

			@Override
			public boolean isRunning() {
				return multipleConcatenationTask != null && multipleConcatenationTask.isAlive();
			}
		});
	}

	private void readConfiguration() {
		ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

		ScheduleConcatConfiguration configuration = configurationManager.get(ScheduleConcatConfiguration.class);

		if (configuration != null) {
			dayComboBox.setSelectedItem(configuration.getSchedule().getDay().name());
			hourSpinner.setValue(configuration.getSchedule().getStartAtTime().getHour());
			minuteSpinner.setValue(configuration.getSchedule().getStartAtTime().getMinute());
		}

		PathConfiguration pathConfiguration = configurationManager.get(PathConfiguration.class);

		if (pathConfiguration != null) {
			autosubTextField.setText(pathConfiguration.getAutosubPath());
		}
	}

	private void storeConfiguration() {
		ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

		ScheduleConcatConfiguration configuration = new ScheduleConcatConfiguration();

		configuration.setSchedule(new Schedule(
				Schedule.Day.valueOf(dayComboBox.getSelectedItem().toString()),
				LocalTime.of(Integer.valueOf(hourSpinner.getValue().toString()), Integer.valueOf(minuteSpinner.getValue().toString())),
				Schedule.ONE_MINUTE));

		configurationManager.add(configuration);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == dayComboBox) {
			storeConfiguration();
		} else if (e.getSource() == addButton) {
			scheduleConcatConfigurationDialog.open();
		} else if (e.getSource() == scheduleConcatConfigurationDialog.saveButton) {
			final ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

			final ScheduleFolderConfiguration configuration =
					scheduleConcatConfigurationDialog.configuration == null
							? new ScheduleFolderConfiguration()
							: new ScheduleFolderConfiguration(scheduleConcatConfigurationDialog.configuration.uid());

			configuration.setFolder(scheduleConcatConfigurationDialog.folderTextField.getText());
			configuration.setLanguage(scheduleConcatConfigurationDialog.languageComboBox.getSelectedItem().toString());
			configuration.setSub(scheduleConcatConfigurationDialog.subCheckBox.isSelected());

			new Thread(() -> {
				SwingUtilities.invokeLater(() -> {
					loadingDialog.setLocationRelativeTo(null);
					loadingDialog.setVisible(true);
				});

				try {
					final ValidationResults validationResults = configurationManager.validate(configuration);

					if (validationResults.fails()) {
						SwingUtilities.invokeLater(() -> {
							JPanel validationPanel = new JPanel();
							validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

							for (ValidationResult validationResult : validationResults) {
								validationPanel.add(new JLabel(validationResult.getMessage()));
							}

							loadingDialog.setVisible(false);
							JOptionPane.showMessageDialog(scheduleConcatConfigurationDialog, validationPanel, "ERROR!", JOptionPane.ERROR_MESSAGE);
						});
					} else {
						configurationManager.add(configuration, true);

						SwingUtilities.invokeLater(() -> {
							loadingDialog.setVisible(false);
							scheduleConcatConfigurationDialog.setVisible(false);
						});
					}
				} finally {
					SwingUtilities.invokeLater(() -> loadingDialog.setVisible(false));
				}
			}).start();
		} else if (e.getSource() == startButton) {
			final boolean enable = !startButton.getText().equals("Start");

			startButton.setText(enable ? "Start" : "Stop");
			updateSchedule();

			SwingUtilities.invokeLater(() -> {
				addButton.setEnabled(enable);
				autosubTextField.setEnabled(enable);
				dayComboBox.setEnabled(enable);
				hourSpinner.setEnabled(enable);
				minuteSpinner.setEnabled(enable);
				folderTable.setEnabled(enable);
			});
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == hourSpinner || e.getSource() == minuteSpinner) {
			storeConfiguration();
		}
	}

	private void showLoading() {
		SwingUtilities.invokeLater(() -> {
			ScheduleConcatenationFrame.this.setEnabled(false);
			loadingDialog.setLocationRelativeTo(this);
			loadingDialog.setVisible(true);
		});
	}

	private void hideLoading() {
		SwingUtilities.invokeLater(() -> {
			ScheduleConcatenationFrame.this.setEnabled(true);
			loadingDialog.setVisible(false);
		});
	}

	private void log(String line) {
		if (line == null) return;

		SwingUtilities.invokeLater(() -> {
			logTextArea.append(line);
			logTextArea.setCaretPosition(logTextArea.getDocument().getLength());

			Pattern conversionPattern = Pattern.compile("Converting.+:\\s*(?<percent>\\d+)%");
			Pattern recognitionPattern = Pattern.compile("Performing.+:\\s*(?<percent>\\d+)%");

			Matcher conversionMatcher = conversionPattern.matcher(line);
			Matcher recognitionMatcher = recognitionPattern.matcher(line);

			if (conversionMatcher.find()) {
				loadingDialog.setAdditionalLoadingText(String.format("Converting: %s%%", conversionMatcher.group("percent")));
			}

			if (recognitionMatcher.find()) {
				loadingDialog.setAdditionalLoadingText(String.format("Speech recognition: %s%%", recognitionMatcher.group("percent")));
			}
		});
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		vSpacer1 = new JPanel(null);
		hSpacer1 = new JPanel(null);
		addButton = new JButton();
		autosubTextField = new JTextField();
		dayComboBox = new JComboBox<>();
		hourSpinner = new JSpinner();
		label1 = new JLabel();
		minuteSpinner = new JSpinner();
		startButton = new JButton();
		hSpacer2 = new JPanel(null);
		scrollPane1 = new JScrollPane();
		folderTable = new JTable();
		scrollPane2 = new JScrollPane();
		logTextArea = new JTextArea();
		vSpacer2 = new JPanel(null);

		//======== this ========
		setTitle("Schedule Concat Tool");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"2*(default, $lcgap), default:grow, 2*($lcgap, default), $lcgap, 2dlu, 3*($lcgap, default)",
			"3*(default, $lgap), 35dlu:grow, $lgap, default"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 11, 1));
		contentPane.add(hSpacer1, CC.xywh(1, 3, 1, 5));

		//---- addButton ----
		addButton.setText("Add Folder");
		contentPane.add(addButton, CC.xy(3, 3));

		//---- autosubTextField ----
		autosubTextField.setToolTipText("Autosub Path");
		autosubTextField.setText("Autosub Path");
		contentPane.add(autosubTextField, CC.xy(5, 3));

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
		contentPane.add(dayComboBox, CC.xy(7, 3));

		//---- hourSpinner ----
		hourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
		contentPane.add(hourSpinner, CC.xy(9, 3));

		//---- label1 ----
		label1.setText(":");
		contentPane.add(label1, CC.xy(11, 3));

		//---- minuteSpinner ----
		minuteSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
		contentPane.add(minuteSpinner, CC.xy(13, 3));

		//---- startButton ----
		startButton.setText("Start");
		contentPane.add(startButton, CC.xy(15, 3));
		contentPane.add(hSpacer2, CC.xywh(17, 3, 1, 5));

		//======== scrollPane1 ========
		{

			//---- folderTable ----
			folderTable.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
					"Folder", "Sub", " ", " "
				}
			) {
				Class<?>[] columnTypes = new Class<?>[] {
					String.class, String.class, Object.class, Object.class
				};
				boolean[] columnEditable = new boolean[] {
					false, false, true, true
				};
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return columnEditable[columnIndex];
				}
			});
			{
				TableColumnModel cm = folderTable.getColumnModel();
				cm.getColumn(0).setPreferredWidth(300);
				cm.getColumn(1).setPreferredWidth(50);
				cm.getColumn(2).setPreferredWidth(100);
				cm.getColumn(3).setPreferredWidth(100);
			}
			scrollPane1.setViewportView(folderTable);
		}
		contentPane.add(scrollPane1, CC.xywh(3, 5, 13, 1));

		//======== scrollPane2 ========
		{

			//---- logTextArea ----
			logTextArea.setEditable(false);
			scrollPane2.setViewportView(logTextArea);
		}
		contentPane.add(scrollPane2, CC.xywh(3, 7, 13, 1, CC.FILL, CC.FILL));
		contentPane.add(vSpacer2, CC.xywh(3, 9, 11, 1));
		setSize(725, 400);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JPanel vSpacer1;
	private JPanel hSpacer1;
	private JButton addButton;
	public JTextField autosubTextField;
	private JComboBox<String> dayComboBox;
	private JSpinner hourSpinner;
	private JLabel label1;
	private JSpinner minuteSpinner;
	private JButton startButton;
	private JPanel hSpacer2;
	private JScrollPane scrollPane1;
	public JTable folderTable;
	private JScrollPane scrollPane2;
	private JTextArea logTextArea;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	private class MultipleConcatenationTask extends Thread implements Runnable {

		private final TaskManager taskManager;
		private final Queue<ThreadedConcatenationTask> threadedConcatenationTasks;

		private final int taskCount;
		private int currentTaskIndex;

		public MultipleConcatenationTask(TaskManager taskManager, Queue<ThreadedConcatenationTask> threadedConcatenationTasks) {
			super(MultipleConcatenationTask.class.getSimpleName());

			this.taskManager = taskManager;
			this.threadedConcatenationTasks = threadedConcatenationTasks;

			taskCount = threadedConcatenationTasks.size();
			currentTaskIndex = 0;
		}

		public boolean notFinished() {
			return !threadedConcatenationTasks.isEmpty();
		}

		@Override
		public void run() {
			try {
				showLoading();

				do {
					ThreadedConcatenationTask threadedConcatenationTask = threadedConcatenationTasks.peek();

					taskManager.add(++currentTaskIndex * -1, threadedConcatenationTask);

					while (threadedConcatenationTask.isFresh() || threadedConcatenationTask.isRunning()) {
						int queueSize = threadedConcatenationTasks.size();
						int completed = taskCount - queueSize;

						int tasksPercent = (completed * 100) / queueSize;

						loadingDialog.setLoadingText(String.format("%d of %d [%d%%]", completed, taskCount, tasksPercent));

						Thread.sleep(1_000L);
					}

					threadedConcatenationTasks.poll();
				} while (!threadedConcatenationTasks.isEmpty());

				hideLoading();
			} catch (Exception ex) {
				log(ex.getMessage());
			}
		}
	}
}
