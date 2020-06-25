/*
 * Created by JFormDesigner on Wed Jun 24 23:20:43 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author unknown
 */
public class LoadingDialog extends JDialog {
    public LoadingDialog(Window owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        label1 = new JLabel();

        //======== this ========
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setModal(true);
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "center:default:grow",
            "default:grow"));

        //---- label1 ----
        label1.setText("Please wait...");
        contentPane.add(label1, CC.xy(1, 1));
        setSize(315, 105);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JLabel label1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
