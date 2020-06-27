/*
 * Created by JFormDesigner on Thu Jun 25 21:12:24 MDT 2020
 */

package com.github.luischavez.videodownloader.app.gui;

import java.awt.*;
import java.util.stream.Collectors;
import javax.swing.*;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.AppConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author unknown
 */
public class AppConfigurationDialog extends JDialog {

    public AppConfigurationDialog(Window owner) {
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
        final AppConfiguration appConfiguration = context.getSystem().getManager(ConfigurationManager.class).get(AppConfiguration.class);

        if (appConfiguration != null) {
            emailTextField.setText(appConfiguration.getEmail());
            passwordField.setText(CryptoUtils.base64Decode(appConfiguration.getPassword()));
            toTextField.setText(appConfiguration.getDistributionList().stream().collect(Collectors.joining(",")));
            autosubTextField.setText(appConfiguration.getAutosubPath());
        }

        setVisible(true);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        vSpacer1 = new JPanel(null);
        hSpacer1 = new JPanel(null);
        label1 = new JLabel();
        emailTextField = new JTextField();
        label2 = new JLabel();
        passwordField = new JPasswordField();
        hSpacer2 = new JPanel(null);
        label3 = new JLabel();
        toTextField = new JTextField();
        separator1 = new JSeparator();
        label4 = new JLabel();
        autosubTextField = new JTextField();
        autosubButton = new JButton();
        vSpacer3 = new JPanel(null);
        vSpacer4 = new JPanel(null);
        saveButton = new JButton();
        vSpacer2 = new JPanel(null);

        //======== this ========
        setTitle("Configuration");
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "2*(default, $lcgap), default:grow, 2*($lcgap, default)",
            "8*(default, $lgap), bottom:default:grow, $lgap, default"));
        contentPane.add(vSpacer1, CC.xywh(3, 1, 5, 1));
        contentPane.add(hSpacer1, CC.xywh(1, 3, 1, 16));

        //---- label1 ----
        label1.setText("Email");
        contentPane.add(label1, CC.xy(3, 3));
        contentPane.add(emailTextField, CC.xywh(5, 3, 3, 1));

        //---- label2 ----
        label2.setText("Password");
        contentPane.add(label2, CC.xy(3, 5));
        contentPane.add(passwordField, CC.xywh(5, 5, 3, 1));
        contentPane.add(hSpacer2, CC.xywh(9, 3, 1, 9));

        //---- label3 ----
        label3.setText("To");
        contentPane.add(label3, CC.xy(3, 7));
        contentPane.add(toTextField, CC.xywh(5, 7, 3, 1));
        contentPane.add(separator1, CC.xywh(3, 9, 5, 1));

        //---- label4 ----
        label4.setText("Autosub");
        contentPane.add(label4, CC.xy(3, 11));

        //---- autosubTextField ----
        autosubTextField.setEditable(false);
        contentPane.add(autosubTextField, CC.xy(5, 11));

        //---- autosubButton ----
        autosubButton.setText("Browse");
        contentPane.add(autosubButton, CC.xy(7, 11));
        contentPane.add(vSpacer3, CC.xy(5, 13));
        contentPane.add(vSpacer4, CC.xy(5, 15));

        //---- saveButton ----
        saveButton.setText("Save");
        contentPane.add(saveButton, CC.xywh(3, 17, 5, 1));
        contentPane.add(vSpacer2, CC.xywh(3, 19, 5, 1));
        setSize(400, 275);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JPanel vSpacer1;
    private JPanel hSpacer1;
    private JLabel label1;
    public JTextField emailTextField;
    private JLabel label2;
    public JPasswordField passwordField;
    private JPanel hSpacer2;
    private JLabel label3;
    public JTextField toTextField;
    private JSeparator separator1;
    private JLabel label4;
    public JTextField autosubTextField;
    public JButton autosubButton;
    private JPanel vSpacer3;
    private JPanel vSpacer4;
    public JButton saveButton;
    private JPanel vSpacer2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
