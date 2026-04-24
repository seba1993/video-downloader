package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

public class YouTubeConfigurationPanel extends JPanel {

	public YouTubeConfigurationPanel() {
		initComponents();

		browseButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int option = fileChooser.showOpenDialog(this);

			if (option == JFileChooser.APPROVE_OPTION) {
				directoryTextField.setText(fileChooser.getSelectedFile().getPath());
			}
		});
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		label1 = new JLabel();
		directoryTextField = new JTextField();
		browseButton = new JButton();
		scrollPane1 = new JScrollPane();
		channelTextArea = new JTextArea();

		//======== this ========
		setMinimumSize(new Dimension(450, 400));
		setPreferredSize(new Dimension(450, 400));
		setMaximumSize(new Dimension(450, 400));
		setLayout(new FormLayout(
			"default, $lcgap, default:grow, $lcgap, default",
			"default, $lgap, default:grow, $lgap, default"));

		//---- label1 ----
		label1.setText("Download to");
		add(label1, CC.xy(1, 1));

		//---- directoryTextField ----
		directoryTextField.setEditable(false);
		add(directoryTextField, CC.xy(3, 1));

		//---- browseButton ----
		browseButton.setText("Browse");
		add(browseButton, CC.xy(5, 1));

		//======== scrollPane1 ========
		{
			scrollPane1.setViewportView(channelTextArea);
		}
		add(scrollPane1, CC.xywh(1, 3, 5, 3));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	private JLabel label1;
	public JTextField directoryTextField;
	public JButton browseButton;
	private JScrollPane scrollPane1;
	public JTextArea channelTextArea;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
