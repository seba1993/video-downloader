package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.gui.ScheduleConcatenationFrame;
import com.github.luischavez.videodownloader.app.manager.GuiRepaintManager;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.configuration.SerializerConfigurationManager;
import com.github.luischavez.videodownloader.schedule.DefaultScheduleManager;
import com.github.luischavez.videodownloader.schedule.ScheduleManager;
import com.github.luischavez.videodownloader.task.DefaultTaskManager;
import com.github.luischavez.videodownloader.task.TaskManager;
import org.pushingpixels.substance.api.skin.SubstanceNightShadeLookAndFeel;

import javax.swing.*;

public class ScheduleConcatenationTool {

    private final DummyContext context;

    private ScheduleConcatenationFrame scheduleConcatenationFrame;

    public ScheduleConcatenationTool() {
        context = DummyContext.instance();
    }

    public void start() {
        context.getSystem().getDependencyInjection().configure(dependencyRegister -> {
            dependencyRegister.bindClass(Context.class).toInstance(context);
            dependencyRegister.bindClass(ConfigurationManager.class).toClass(SerializerConfigurationManager.class);
            dependencyRegister.bindClass(ScheduleManager.class).toClass(DefaultScheduleManager.class);
            dependencyRegister.bindClass(TaskManager.class).toClass(DefaultTaskManager.class);
        });

        context.getSystem().registerManager(ConfigurationManager.class);
        context.getSystem().registerManager(ScheduleManager.class);
        context.getSystem().registerManager(TaskManager.class);
        context.getSystem().registerManager(GuiRepaintManager.class);

        ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);
        ((SerializerConfigurationManager) configurationManager).setConfigurationFolder("monitor");

        context.getSystem().startAllManagers(true);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceNightShadeLookAndFeel());
            } catch (Exception ex) {
                // IGNORE
            }

            scheduleConcatenationFrame = new ScheduleConcatenationFrame(context);
            scheduleConcatenationFrame.setVisible(true);

            context.getSystem().getManager(GuiRepaintManager.class).addComponent(scheduleConcatenationFrame.folderTable);
        });
    }

    public static void main(String... args) {
        new ScheduleConcatenationTool().start();
    }
}
