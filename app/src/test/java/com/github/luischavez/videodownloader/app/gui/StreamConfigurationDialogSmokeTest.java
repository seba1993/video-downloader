package com.github.luischavez.videodownloader.app.gui;

import javax.swing.SwingUtilities;

public class StreamConfigurationDialogSmokeTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StreamConfigurationDialog dialog = new StreamConfigurationDialog(null);
            if (dialog.timeZoneComboBox.getItemCount() == 0) {
                throw new AssertionError("time zone list is empty");
            }
            if (dialog.dailySplitHourSpinner.isEnabled() || dialog.dailySplitMinuteSpinner.isEnabled()) {
                throw new AssertionError("daily split controls should start disabled");
            }
            dialog.dispose();
        });

        java.lang.System.out.println("StreamConfigurationDialogSmokeTest passed");
        java.lang.System.exit(0);
    }
}
