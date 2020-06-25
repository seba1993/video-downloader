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
        // Generated using JFormDesigner Evaluation license - unknown
        vSpacer1 = new JPanel(null);
        addButton = new JButton();
        hSpacer3 = new JPanel(null);
        enableAllButton = new JButton();
        disableAllButton = new JButton();
        label1 = new JLabel();
        searchTextField = new JTextField();
        hSpacer1 = new JPanel(null);
        streamScrollPane = new JScrollPane();
        streamTable = new JTable();
        hSpacer2 = new JPanel(null);

        //======== this ========
        addPropertyChangeListener(new java.beans.PropertyChangeListener(){@Override public void propertyChange(java.
        beans.PropertyChangeEvent e){if("\u0062ord\u0065r".equals(e.getPropertyName()))throw new RuntimeException();}});
        setLayout(new FormLayout(
            "3*(default, $lcgap), left:default, $lcgap, default, $lcgap, right:default:grow, $lcgap, default:grow, $lcgap, right:default:grow, $lcgap, pref:grow, $lcgap, default",
            "2*(default, $lgap), fill:default:grow, $lgap, fill:25dlu"));
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

        //---- label1 ----
        label1.setText("Search");
        add(label1, CC.xy(15, 3));
        add(searchTextField, CC.xy(17, 3));
        add(hSpacer1, CC.xy(1, 5));

        //======== streamScrollPane ========
        {

            //---- streamTable ----
            streamTable.setModel(new DefaultTableModel(
                new Object[][] {
                },
                new String[] {
                    "Country", "Alias", "Status", "Download", "Time", null, null, null
                }
            ) {
                boolean[] columnEditable = new boolean[] {
                    false, false, false, false, false, true, true, true
                };
                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnEditable[columnIndex];
                }
            });
            {
                TableColumnModel cm = streamTable.getColumnModel();
                cm.getColumn(0).setMinWidth(80);
                cm.getColumn(0).setPreferredWidth(80);
                cm.getColumn(1).setMinWidth(100);
                cm.getColumn(1).setPreferredWidth(100);
                cm.getColumn(2).setMinWidth(300);
                cm.getColumn(2).setPreferredWidth(300);
                cm.getColumn(3).setMinWidth(150);
                cm.getColumn(3).setPreferredWidth(150);
                cm.getColumn(4).setMinWidth(150);
                cm.getColumn(4).setPreferredWidth(150);
                cm.getColumn(5).setMinWidth(70);
                cm.getColumn(5).setPreferredWidth(70);
                cm.getColumn(6).setMinWidth(70);
                cm.getColumn(6).setPreferredWidth(70);
                cm.getColumn(7).setMinWidth(70);
                cm.getColumn(7).setPreferredWidth(70);
            }
            streamTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            streamScrollPane.setViewportView(streamTable);
        }
        add(streamScrollPane, CC.xywh(3, 5, 15, 1));
        add(hSpacer2, CC.xy(19, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel vSpacer1;
    public JButton addButton;
    private JPanel hSpacer3;
    public JButton enableAllButton;
    public JButton disableAllButton;
    private JLabel label1;
    public JTextField searchTextField;
    private JPanel hSpacer1;
    public JScrollPane streamScrollPane;
    public JTable streamTable;
    private JPanel hSpacer2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
