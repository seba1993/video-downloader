package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.PathConfiguration;
import com.github.luischavez.videodownloader.app.gui.ConcatenationFrame;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.configuration.SerializerConfigurationManager;
import com.github.luischavez.videodownloader.task.DefaultTaskManager;
import com.github.luischavez.videodownloader.task.TaskManager;
import org.pushingpixels.substance.api.skin.SubstanceNightShadeLookAndFeel;

import javax.swing.*;

public class ConcatenationTool {

    private final DummyContext context;

    private ConcatenationFrame concatenationFrame;

    public ConcatenationTool() {
        context = DummyContext.instance();
    }

    public void start() {
        context.getSystem().getDependencyInjection().configure(dependencyRegister -> {
            dependencyRegister.bindClass(Context.class).toInstance(context);
            dependencyRegister.bindClass(ConfigurationManager.class).toClass(SerializerConfigurationManager.class);
            dependencyRegister.bindClass(TaskManager.class).toClass(DefaultTaskManager.class);
        });

        context.getSystem().registerManager(ConfigurationManager.class);
        context.getSystem().registerManager(TaskManager.class);
        context.getSystem().startAllManagers(true);

        ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

        context.getSystem().getManager(ConfigurationManager.class).stop();

        PathConfiguration configuration = configurationManager.get(PathConfiguration.class);
        if (configuration == null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(new SubstanceNightShadeLookAndFeel());
                } catch (Exception ex) {
                    // IGNORE
                }

                JOptionPane.showMessageDialog(null, "Configuration not found", "ERROR!", JOptionPane.ERROR_MESSAGE);

                context.getSystem().stopAllManagers(true);
                java.lang.System.exit(1);
            });

            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceNightShadeLookAndFeel());
            } catch (Exception ex) {
                // IGNORE
            }

            concatenationFrame = new ConcatenationFrame(context);
            concatenationFrame.setVisible(true);
        });
    }

    public static void main(String... args) {
        new ConcatenationTool().start();
    }
}
