package com.github.luischavez.videodownloader.streamclient.api.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.*;

import com.github.luischavez.videodownloader.streamclient.Main;
import com.github.luischavez.videodownloader.streamclient.api.model.User;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

public class LoginDialog extends JDialog implements ActionListener {

	public LoginDialog(Window owner) {
		super(owner);
		initComponents();
		initialize();
	}

	private void initialize() {
		loginButton.addActionListener(this);

		Properties properties = Main.readProperties();

		serverTextField.setText(properties.getOrDefault("api_server", "").toString());
		authCodeTextField.setText(properties.getOrDefault("auth_code", "").toString());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loginButton) {
			Properties properties = Main.readProperties();
			properties.setProperty("api_server", serverTextField.getText());
			properties.setProperty("auth_code", authCodeTextField.getText());
			Main.storeProperties(properties);

			User user = Main.getCurrentUser();

			if (user == null) {
				JOptionPane.showMessageDialog(this, "Invalid credentials!", "Login Error", JOptionPane.ERROR_MESSAGE);
			} else {
				new MainFrame().setVisible(true);
				dispose();
			}
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis Chavez
		vSpacer1 = new JPanel(null);
		hSpacer1 = new JPanel(null);
		label1 = new JLabel();
		hSpacer2 = new JPanel(null);
		label2 = new JLabel();
		serverTextField = new JTextField();
		label3 = new JLabel();
		authCodeTextField = new JTextField();
		loginButton = new JButton();
		vSpacer2 = new JPanel(null);

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Login");
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"2*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"5*(default, $lgap), bottom:default:grow, $lgap, default"));
		contentPane.add(vSpacer1, CC.xywh(3, 1, 5, 1));
		contentPane.add(hSpacer1, CC.xywh(1, 3, 1, 7));

		//---- label1 ----
		label1.setText("Login");
		label1.setFont(new Font("Lucida Grande", Font.BOLD, 18));
		contentPane.add(label1, CC.xywh(3, 3, 5, 1, CC.CENTER, CC.DEFAULT));
		contentPane.add(hSpacer2, CC.xywh(9, 3, 1, 7));

		//---- label2 ----
		label2.setText("Server");
		contentPane.add(label2, CC.xy(3, 7));
		contentPane.add(serverTextField, CC.xywh(5, 7, 3, 1));

		//---- label3 ----
		label3.setText("Auth Code");
		contentPane.add(label3, CC.xy(3, 9));
		contentPane.add(authCodeTextField, CC.xywh(5, 9, 3, 1));

		//---- loginButton ----
		loginButton.setText("Login");
		contentPane.add(loginButton, CC.xywh(3, 11, 5, 1));
		contentPane.add(vSpacer2, CC.xywh(3, 13, 5, 1));
		setSize(400, 300);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis Chavez
	private JPanel vSpacer1;
	private JPanel hSpacer1;
	private JLabel label1;
	private JPanel hSpacer2;
	private JLabel label2;
	private JTextField serverTextField;
	private JLabel label3;
	private JTextField authCodeTextField;
	private JButton loginButton;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
