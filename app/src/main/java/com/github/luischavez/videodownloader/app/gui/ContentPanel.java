/*
 * Created by JFormDesigner on Mon Jun 22 17:12:18 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import javax.swing.*;
import javax.swing.table.*;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author unknown
 */
public class ContentPanel extends JPanel {

    public ContentPanel() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Luis
		vSpacer1 = new JPanel(null);
		addButton = new JButton();
		hSpacer3 = new JPanel(null);
		enableAllButton = new JButton();
		disableAllButton = new JButton();
		ytButton = new JButton();
		label1 = new JLabel();
		searchTextField = new JTextField();
		selectionCheckBox = new JCheckBox();
		configureSelectedButton = new JButton();
		deleteDisabled = new JButton();
		hSpacer1 = new JPanel(null);
		streamScrollPane = new JScrollPane();
		streamTable = new JTable();
		hSpacer2 = new JPanel(null);

		//======== this ========
		setLayout(new FormLayout(
			"3*(default, $lcgap), left:default, $lcgap, default, $lcgap, 51dlu, $lcgap, right:default:grow, $lcgap, default:grow, $lcgap, right:default:grow, $lcgap, pref:grow, $lcgap, default",
			"3*(default, $lgap), fill:default:grow, $lgap, fill:25dlu"));
		add(vSpacer1, CC.xy(7, 1));

		//---- addButton ----
		addButton.setText("Add");
		add(addButton, CC.xy(3, 3));
		add(hSpacer3, CC.xy(5, 3));

		//---- enableAllButton ----
		enableAllButton.setText("Enable All");
		add(enableAllButton, CC.xy(7, 3));

		//---- disableAllButton ----
		disableAllButton.setText("Disable All");
		add(disableAllButton, CC.xy(9, 3));

		//---- ytButton ----
		ytButton.setText("YouTube");
		add(ytButton, CC.xy(15, 3));

		//---- label1 ----
		label1.setText("Search");
		add(label1, CC.xy(17, 3));
		add(searchTextField, CC.xy(19, 3));

		//---- selectionCheckBox ----
		selectionCheckBox.setText("Toggle Selection");
		add(selectionCheckBox, CC.xywh(3, 5, 5, 1));

		//---- configureSelectedButton ----
		configureSelectedButton.setText("Configure Selected");
		configureSelectedButton.setVisible(false);
		add(configureSelectedButton, CC.xywh(9, 5, 3, 1));

		//---- deleteDisabled ----
		deleteDisabled.setText("Delete disabled");
		deleteDisabled.setVisible(false);
		add(deleteDisabled, CC.xy(13, 5, CC.CENTER, CC.DEFAULT));
		add(hSpacer1, CC.xy(1, 7));

		//======== streamScrollPane ========
		{

			//---- streamTable ----
			streamTable.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
					" ", "Country", "Alias", "Status", "Download", "Time", null, null, null
				}
			) {
				boolean[] columnEditable = new boolean[] {
					true, false, false, false, false, false, true, true, true
				};
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return columnEditable[columnIndex];
				}
			});
			{
				TableColumnModel cm = streamTable.getColumnModel();
				cm.getColumn(0).setMinWidth(50);
				cm.getColumn(0).setPreferredWidth(50);
				cm.getColumn(1).setMinWidth(80);
				cm.getColumn(1).setPreferredWidth(80);
				cm.getColumn(2).setMinWidth(100);
				cm.getColumn(2).setPreferredWidth(100);
				cm.getColumn(3).setMinWidth(300);
				cm.getColumn(3).setPreferredWidth(300);
				cm.getColumn(4).setMinWidth(150);
				cm.getColumn(4).setPreferredWidth(150);
				cm.getColumn(5).setMinWidth(150);
				cm.getColumn(5).setPreferredWidth(150);
				cm.getColumn(6).setMinWidth(70);
				cm.getColumn(6).setPreferredWidth(70);
				cm.getColumn(7).setMinWidth(70);
				cm.getColumn(7).setPreferredWidth(70);
				cm.getColumn(8).setMinWidth(70);
				cm.getColumn(8).setPreferredWidth(70);
			}
			streamTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			streamScrollPane.setViewportView(streamTable);
		}
		add(streamScrollPane, CC.xywh(3, 7, 17, 1));
		add(hSpacer2, CC.xy(21, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Luis
	private JPanel vSpacer1;
	public JButton addButton;
	private JPanel hSpacer3;
	public JButton enableAllButton;
	public JButton disableAllButton;
	public JButton ytButton;
	private JLabel label1;
	public JTextField searchTextField;
	public JCheckBox selectionCheckBox;
	public JButton configureSelectedButton;
	public JButton deleteDisabled;
	private JPanel hSpacer1;
	public JScrollPane streamScrollPane;
	public JTable streamTable;
	private JPanel hSpacer2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
