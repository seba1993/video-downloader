package com.github.luischavez.videodownloader.streamclient.api.gui;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

public class VideoDialog extends JDialog {

	public VideoDialog(Window owner) {
		super(owner);
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		vSpacer1 = new JPanel(null);
		videoTitleLabel = new JLabel();
		hSpacer1 = new JPanel(null);
		hSpacer2 = new JPanel(null);
		videoPanel = new VideoPanel();
		vSpacer2 = new JPanel(null);

		//======== this ========
		Container contentPane = getContentPane();
		contentPane.setLayout(new FormLayout(
			"3*(default, $lcgap), default:grow, 2*($lcgap, default)",
			"3*(default, $lgap), default:grow, $lgap, default"));
		contentPane.add(vSpacer1, CC.xy(7, 1));

		//---- videoTitleLabel ----
		videoTitleLabel.setText("VIDEO NAME");
		contentPane.add(videoTitleLabel, CC.xywh(3, 3, 7, 1, CC.CENTER, CC.DEFAULT));
		contentPane.add(hSpacer1, CC.xy(1, 5));
		contentPane.add(hSpacer2, CC.xy(11, 5));
		contentPane.add(videoPanel, CC.xywh(3, 5, 7, 3));
		contentPane.add(vSpacer2, CC.xy(7, 9));
		setSize(1075, 500);
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	private JPanel vSpacer1;
	public JLabel videoTitleLabel;
	private JPanel hSpacer1;
	private JPanel hSpacer2;
	public VideoPanel videoPanel;
	private JPanel vSpacer2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
