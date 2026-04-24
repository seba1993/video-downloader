/*
 * Created by JFormDesigner on Thu Jun 25 11:32:13 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;
import com.github.luischavez.videodownloader.app.task.ThreadedConcatenationTask;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.jdesktop.beansbinding.*;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;

/**
 * @author unknown
 */
public class ConcatenationFrame extends JFrame implements ActionListener {

    private final JFileChooser chooser;
    private final LoadingDialog loadingDialog;

    private final Context context;

    public ConcatenationFrame(Context context) {
        initComponents();

        this.context = context;

        chooser = new JFileChooser();
        loadingDialog = new LoadingDialog(this);

        initialize();
    }

    private void showLoading() {
        SwingUtilities.invokeLater(() -> {
            ConcatenationFrame.this.setEnabled(false);
            loadingDialog.setLocationRelativeTo(this);
            loadingDialog.setVisible(true);
        });
    }

    private void hideLoading() {
        SwingUtilities.invokeLater(() -> {
            ConcatenationFrame.this.setEnabled(true);
            loadingDialog.setVisible(false);
        });
    }

    private void log(String line) {
        if (line == null) return;

        SwingUtilities.invokeLater(() -> {
            logTextArea.append(line);
            logTextArea.append("\n");
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

    private final void initialize() {
        setLocationRelativeTo(null);

        actionComboBox.addActionListener(this);
        sourceBrowseButton.addActionListener(this);
        destinationBrowseButton.addActionListener(this);
        actionButton.addActionListener(this);
    }

    private ThreadedConcatenationTask buildConcatenationTask(File sourceFile, String extension, File destinationFile, String language, boolean concatenate, boolean sub, boolean extractAudio, boolean cleanFiles) {
        ThreadedConcatenationTask.ConcatenationTaskBuilder builder
                = new ThreadedConcatenationTask.ConcatenationTaskBuilder(context);

        if (concatenate) builder.concatenate();
        if (sub) builder.sub();
        if (extractAudio) builder.extractAudio();
        if (cleanFiles) builder.cleanFiles();

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

    private List<File> listAllFiles(File sourceFile, String extension, boolean concatenate, boolean sub) {
        File[] files = sourceFile.listFiles((dir, name) -> {
            File file = new File(dir, name);

            if (file.isDirectory()) {
                return true;
            } else {
                return file.getName().toUpperCase().endsWith(extension.toUpperCase());
            }
        });

        ArrayList<File> listFiles = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                for (File parentFile : listAllFiles(file, extension, concatenate, sub)) {
                    if (!listFiles.contains(parentFile)) {
                        listFiles.add(parentFile);
                    }
                }
            } else {
                if (sub && !concatenate) {
                    listFiles.add(file);
                } else {
                    File parentFile = file.getParentFile();

                    if (!listFiles.contains(parentFile)) {
                        listFiles.add(parentFile);
                    }
                }
            }
        }

        return listFiles;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final String extension = typeComboBox.getSelectedItem().toString();
        final String language = subComboBox.getSelectedItem().toString();
        final boolean concatenate = actionComboBox.getSelectedItem().toString().toLowerCase().contains("concatenate");
        final boolean sub = actionComboBox.getSelectedItem().toString().toLowerCase().contains("sub");
        final boolean extractAudio = extractAudioCheckBox.isSelected();
        final boolean deleteFiles = deleteFilesCheckBox.isSelected();

        if (e.getSource() == sourceBrowseButton) {
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(concatenate ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setFileFilter(new FileNameExtensionFilter(extension, extension));

            int option = chooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();

                sourceTextField.setText(selectedFile.getPath());

                if (selectedFile.isDirectory()) {
                    destinationTextField.setText(selectedFile.getPath());
                } else {
                    destinationTextField.setText("");
                }

                actionButton.setEnabled(selectedFile.isDirectory());
            }
        } else if (e.getSource() == destinationBrowseButton) {
            File sourceFile = new File(sourceTextField.getText());

            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(sourceFile.isFile() ? JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);

            if (sourceFile.isFile()) {
                chooser.setFileFilter(new FileNameExtensionFilter(sub ? "srt" : extension, sub ? "srt" : extension));
            }

            int option = chooser.showSaveDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                String destinationFilePath = selectedFile.getPath();
                
                destinationTextField.setText(destinationFilePath);
                actionButton.setEnabled(true);
            }
        } else if (e.getSource() == actionButton) {
            final TaskManager taskManager = context.getSystem().getManager(TaskManager.class);

            final File sourceFile = new File(sourceTextField.getText());
            final File destinationFile = new File(destinationTextField.getText());

            if (sourceFile.isDirectory()) {
                List<File> fileList = listAllFiles(sourceFile, extension, concatenate, sub);

                if (fileList != null && fileList.size() > 0) {
                    Queue<ThreadedConcatenationTask> threadedConcatenationTasks = new ArrayDeque<>();

                    for (int i = 0; i < fileList.size(); i++) {
                        File file = fileList.get(i);
                        String parent = file.getParentFile().getName();
                        String[] sName = file.getName().split("\\.");
                        String fileName = sName[0];

                        File calculatedDestinationFile = new File(destinationFile, String.format("%s-%s.%s", parent, fileName, extension));

                        if (calculatedDestinationFile.exists()) {
                            continue;
                        }

                        threadedConcatenationTasks.add(buildConcatenationTask(file, extension, calculatedDestinationFile, language, concatenate, sub, extractAudio, deleteFiles));
                    }

                    MultipleConcatenationTask multipleConcatenationTask = new MultipleConcatenationTask(taskManager, threadedConcatenationTasks);
                    multipleConcatenationTask.start();
                }
            } else {
                taskManager.add(-100, buildConcatenationTask(sourceFile, extension, destinationFile, language, concatenate, sub, extractAudio, deleteFiles));
            }
        } else if (e.getSource() == actionComboBox) {
            sourceTextField.setText("");
            destinationTextField.setText("");
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		vSpacer1 = new JPanel(null);
		label4 = new JLabel();
		actionComboBox = new JComboBox<>();
		deleteFilesCheckBox = new JCheckBox();
		label5 = new JLabel();
		typeComboBox = new JComboBox<>();
		extractAudioCheckBox = new JCheckBox();
		label1 = new JLabel();
		sourceTextField = new JTextField();
		sourceBrowseButton = new JButton();
		label2 = new JLabel();
		destinationTextField = new JTextField();
		destinationBrowseButton = new JButton();
		label3 = new JLabel();
		hSpacer1 = new JPanel(null);
		hSpacer2 = new JPanel(null);
		subComboBox = new JComboBox<>();
		separator1 = new JSeparator();
		actionButton = new JButton();
		scrollPane2 = new JScrollPane();
		logTextArea = new JTextArea();
		vSpacer2 = new JPanel(null);

		//======== this ========
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"5*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"9*(default, $lgap), 47dlu:grow, $lgap, default"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 11, 1));

		//---- label4 ----
		label4.setText("Action");
		contentPane.add(label4, CC.xy(3, 3));

		//---- actionComboBox ----
		actionComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
			"Concatenate & Sub",
			"Concatenate",
			"Sub"
		}));
		contentPane.add(actionComboBox, CC.xy(5, 3));

		//---- deleteFilesCheckBox ----
		deleteFilesCheckBox.setText("Delete files?");
		contentPane.add(deleteFilesCheckBox, CC.xywh(7, 3, 5, 1));

		//---- label5 ----
		label5.setText("Type");
		contentPane.add(label5, CC.xy(3, 5));

		//---- typeComboBox ----
		typeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
			"mkv",
			"mp3"
		}));
		contentPane.add(typeComboBox, CC.xy(5, 5));

		//---- extractAudioCheckBox ----
		extractAudioCheckBox.setText("Extract audio?");
		contentPane.add(extractAudioCheckBox, CC.xywh(7, 5, 5, 1));

		//---- label1 ----
		label1.setText("Source");
		contentPane.add(label1, CC.xy(3, 9));

		//---- sourceTextField ----
		sourceTextField.setEditable(false);
		contentPane.add(sourceTextField, CC.xywh(5, 9, 7, 1));

		//---- sourceBrowseButton ----
		sourceBrowseButton.setText("Browse");
		contentPane.add(sourceBrowseButton, CC.xy(13, 9));

		//---- label2 ----
		label2.setText("Destination");
		contentPane.add(label2, CC.xy(3, 11));

		//---- destinationTextField ----
		destinationTextField.setEditable(false);
		contentPane.add(destinationTextField, CC.xywh(5, 11, 7, 1));

		//---- destinationBrowseButton ----
		destinationBrowseButton.setText("Browse");
		contentPane.add(destinationBrowseButton, CC.xy(13, 11));

		//---- label3 ----
		label3.setText("Sub");
		contentPane.add(label3, CC.xy(3, 13));
		contentPane.add(hSpacer1, CC.xywh(15, 11, 1, 7));
		contentPane.add(hSpacer2, CC.xywh(1, 11, 1, 7));

		//---- subComboBox ----
		subComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
			"Spanish",
			"English",
			"French",
			"Portuguese"
		}));
		contentPane.add(subComboBox, CC.xy(5, 13));
		contentPane.add(separator1, CC.xywh(3, 15, 11, 1));
		contentPane.add(actionButton, CC.xywh(5, 17, 7, 1));

		//======== scrollPane2 ========
		{

			//---- logTextArea ----
			logTextArea.setEditable(false);
			scrollPane2.setViewportView(logTextArea);
		}
		contentPane.add(scrollPane2, CC.xywh(3, 19, 11, 1, CC.FILL, CC.FILL));
		contentPane.add(vSpacer2, CC.xywh(3, 21, 11, 1));
		setSize(655, 610);
		setLocationRelativeTo(getOwner());

		//---- bindings ----
		bindingGroup = new BindingGroup();
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, BeanProperty.create("selectedItem"),
			actionButton, BeanProperty.create("text")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, ELProperty.create("${selectedItem == \"Concatenate & Sub\" || selectedItem == \"Sub\"}"),
			label3, BeanProperty.create("visible")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, BeanProperty.create("selectedItem"),
			this, BeanProperty.create("title")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ,
			sourceTextField, ELProperty.create("${text != \"\"}"),
			destinationBrowseButton, BeanProperty.create("enabled")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, ELProperty.create("${selectedItem == \"Concatenate & Sub\" || selectedItem == \"Sub\"}"),
			subComboBox, BeanProperty.create("visible")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, ELProperty.create("${selectedItem == \"Concatenate & Sub\" || selectedItem == \"Concatenate\"}"),
			deleteFilesCheckBox, BeanProperty.create("visible")));
		bindingGroup.addBinding(Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
			actionComboBox, ELProperty.create("${selectedItem == \"Concatenate & Sub\" || selectedItem == \"Concatenate\"}"),
			extractAudioCheckBox, BeanProperty.create("visible")));
		bindingGroup.bind();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	private JPanel vSpacer1;
	private JLabel label4;
	private JComboBox<String> actionComboBox;
	private JCheckBox deleteFilesCheckBox;
	private JLabel label5;
	private JComboBox<String> typeComboBox;
	private JCheckBox extractAudioCheckBox;
	private JLabel label1;
	private JTextField sourceTextField;
	private JButton sourceBrowseButton;
	private JLabel label2;
	private JTextField destinationTextField;
	private JButton destinationBrowseButton;
	private JLabel label3;
	private JPanel hSpacer1;
	private JPanel hSpacer2;
	private JComboBox<String> subComboBox;
	private JSeparator separator1;
	private JButton actionButton;
	private JScrollPane scrollPane2;
	private JTextArea logTextArea;
	private JPanel vSpacer2;
	private BindingGroup bindingGroup;
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

                    taskManager.add(currentTaskIndex++ * -1, threadedConcatenationTask);

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
