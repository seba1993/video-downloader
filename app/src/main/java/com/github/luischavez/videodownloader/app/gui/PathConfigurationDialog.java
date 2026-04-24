package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.util.stream.Collectors;
import javax.swing.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.AppConfiguration;
import com.github.luischavez.videodownloader.app.configuration.PathConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Luis Chavez
 */
public class PathConfigurationDialog extends JDialog {

	public PathConfigurationDialog(Window owner) {
		super(owner);

		initComponents();

		initialize();
	}

	private final void initialize() {
		autosubButton.addActionListener((actionEvent) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);

			int option = chooser.showOpenDialog(this);

			if (option == JFileChooser.APPROVE_OPTION) {
				autosubTextField.setText(chooser.getSelectedFile().getPath());
			}
		});
	}

	public void open(Context context) {
		final PathConfiguration configuration = context.getSystem().getManager(ConfigurationManager.class).get(PathConfiguration.class);

		if (configuration != null) {
			autosubButton.setText(configuration.getAutosubPath());
		}

		setVisible(true);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		vSpacer1 = new JPanel(null);
		label4 = new JLabel();
		autosubTextField = new JTextField();
		autosubButton = new JButton();
		saveButton = new JButton();
		vSpacer2 = new JPanel(null);

		//======== this ========
		setTitle("Path");
		setResizable(false);
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"2*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"2*(default, $lgap), bottom:default:grow, $lgap, default"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 5, 1));

		//---- label4 ----
		label4.setText("Autosub");
		contentPane.add(label4, CC.xy(3, 3));

		//---- autosubTextField ----
		autosubTextField.setEditable(false);
		contentPane.add(autosubTextField, CC.xy(5, 3));

		//---- autosubButton ----
		autosubButton.setText("Browse");
		contentPane.add(autosubButton, CC.xy(7, 3));

		//---- saveButton ----
		saveButton.setText("Save");
		contentPane.add(saveButton, CC.xywh(3, 5, 5, 1));
		contentPane.add(vSpacer2, CC.xywh(3, 7, 5, 1));
		setSize(400, 120);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JPanel vSpacer1;
	private JLabel label4;
	public JTextField autosubTextField;
	public JButton autosubButton;
	public JButton saveButton;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
