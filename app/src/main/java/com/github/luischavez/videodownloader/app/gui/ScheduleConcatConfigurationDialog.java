package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.ScheduleFolderConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.google.inject.internal.cglib.core.$ClassInfo;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Luis Chavez
 */
public class ScheduleConcatConfigurationDialog extends JDialog implements ActionListener {

	private final Context context;

	public ScheduleFolderConfiguration configuration;

	public ScheduleConcatConfigurationDialog(Window owner, Context context) {
		super(owner);

		this.context = context;

		initComponents();

		browseButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == browseButton) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int option = chooser.showOpenDialog(this);

			if (option == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();

				folderTextField.setText(file.getPath());
			}
		}
	}

	public void open(String folder) {
		configuration = null;

		if (folder != null) {
			List<ScheduleFolderConfiguration> configurations
					= context.getSystem().getManager(ConfigurationManager.class).list(ScheduleFolderConfiguration.class);

			if (configurations != null && !configurations.isEmpty()) {
				configuration = configurations.stream()
						.filter(c -> c.getFolder().equals(folder))
						.findFirst()
						.orElse(null);

				if (configuration != null) {
					folderTextField.setText(configuration.getFolder());
					languageComboBox.setSelectedItem(configuration.getLanguage());
					subCheckBox.setSelected(configuration.isSub());
				}
			}
		}

		setVisible(true);
	}

	public void open() {
		open(null);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		vSpacer2 = new JPanel(null);
		hSpacer1 = new JPanel(null);
		label1 = new JLabel();
		folderTextField = new JTextField();
		browseButton = new JButton();
		hSpacer2 = new JPanel(null);
		label2 = new JLabel();
		languageComboBox = new JComboBox<>();
		subCheckBox = new JCheckBox();
		saveButton = new JButton();
		vSpacer1 = new JPanel(null);

		//======== this ========
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"2*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"5*(default, $lgap), bottom:default:grow, $lgap, default"));
		contentPane.add(vSpacer2, CC.xywh(3, 1, 5, 1));
		contentPane.add(hSpacer1, CC.xywh(1, 3, 1, 7));

		//---- label1 ----
		label1.setText("Folder");
		contentPane.add(label1, CC.xy(3, 3));
		contentPane.add(folderTextField, CC.xy(5, 3));

		//---- browseButton ----
		browseButton.setText("Browse");
		contentPane.add(browseButton, CC.xy(7, 3));
		contentPane.add(hSpacer2, CC.xywh(9, 3, 1, 7));

		//---- label2 ----
		label2.setText("Language");
		contentPane.add(label2, CC.xy(3, 5));

		//---- languageComboBox ----
		languageComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
			"Spanish",
			"English",
			"French",
			"Portuguese"
		}));
		contentPane.add(languageComboBox, CC.xywh(5, 5, 3, 1));

		//---- subCheckBox ----
		subCheckBox.setText("Sub");
		contentPane.add(subCheckBox, CC.xywh(3, 7, 5, 1));

		//---- saveButton ----
		saveButton.setText("Save");
		contentPane.add(saveButton, CC.xywh(3, 11, 5, 1));
		contentPane.add(vSpacer1, CC.xywh(3, 13, 5, 1));
		setSize(400, 195);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JPanel vSpacer2;
	private JPanel hSpacer1;
	private JLabel label1;
	public JTextField folderTextField;
	public JButton browseButton;
	private JPanel hSpacer2;
	private JLabel label2;
	public JComboBox<String> languageComboBox;
	public JCheckBox subCheckBox;
	public JButton saveButton;
	private JPanel vSpacer1;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
