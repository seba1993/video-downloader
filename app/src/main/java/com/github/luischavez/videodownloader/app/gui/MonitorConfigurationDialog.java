package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import javax.swing.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.MonitorConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Luis Chavez
 */
public class MonitorConfigurationDialog extends JDialog {

	public MonitorConfigurationDialog(Window owner) {
		super(owner);
		initComponents();
	}

	public void open(Context context) {
		final MonitorConfiguration monitorConfiguration
				= context.getSystem().getManager(ConfigurationManager.class).get(MonitorConfiguration.class);

		if (monitorConfiguration != null) {
			enableCheckBox.setSelected(monitorConfiguration.isEnabled());
			urlTextField.setText(monitorConfiguration.getUrl());
			codeTextField.setText(monitorConfiguration.getCode());
			refreshIntervalSpinner.setValue(monitorConfiguration.getRefreshInterval());
		}

		setVisible(true);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		vSpacer1 = new JPanel(null);
		hSpacer1 = new JPanel(null);
		enableCheckBox = new JCheckBox();
		hSpacer2 = new JPanel(null);
		label1 = new JLabel();
		urlTextField = new JTextField();
		label2 = new JLabel();
		codeTextField = new JTextField();
		label3 = new JLabel();
		refreshIntervalSpinner = new JSpinner();
		saveButton = new JButton();
		vSpacer2 = new JPanel(null);

		//======== this ========
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"2*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"5*(default, $lgap), default:grow, 2*($lgap, default)"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 5, 1));
		contentPane.add(hSpacer1, CC.xywh(1, 3, 1, 5));

		//---- enableCheckBox ----
		enableCheckBox.setText("Enabled");
		contentPane.add(enableCheckBox, CC.xywh(3, 3, 3, 1));
		contentPane.add(hSpacer2, CC.xywh(9, 3, 1, 5));

		//---- label1 ----
		label1.setText("Monitor Server");
		contentPane.add(label1, CC.xy(3, 5));
		contentPane.add(urlTextField, CC.xywh(5, 5, 3, 1));

		//---- label2 ----
		label2.setText("Auth Code");
		contentPane.add(label2, CC.xy(3, 7));
		contentPane.add(codeTextField, CC.xywh(5, 7, 3, 1));

		//---- label3 ----
		label3.setText("Refresh Interval");
		contentPane.add(label3, CC.xy(3, 9));

		//---- refreshIntervalSpinner ----
		refreshIntervalSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
		contentPane.add(refreshIntervalSpinner, CC.xywh(5, 9, 3, 1));

		//---- saveButton ----
		saveButton.setText("Save");
		contentPane.add(saveButton, CC.xywh(3, 13, 5, 1));
		contentPane.add(vSpacer2, CC.xywh(3, 15, 5, 1));
		setSize(405, 220);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JPanel vSpacer1;
	private JPanel hSpacer1;
	public JCheckBox enableCheckBox;
	private JPanel hSpacer2;
	private JLabel label1;
	public JTextField urlTextField;
	private JLabel label2;
	public JTextField codeTextField;
	private JLabel label3;
	public JSpinner refreshIntervalSpinner;
	public JButton saveButton;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
