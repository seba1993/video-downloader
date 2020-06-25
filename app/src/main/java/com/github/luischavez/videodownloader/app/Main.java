package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.gui.MainFrame;
import com.github.luischavez.videodownloader.app.gui.model.StreamTableModel;
import com.github.luischavez.videodownloader.app.manager.GuiRepaintManager;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.app.task.ScheduleStreamTask;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.configuration.SerializerConfigurationManager;
import com.github.luischavez.videodownloader.schedule.*;
import com.github.luischavez.videodownloader.manager.support.PluggableSupportManager;
import com.github.luischavez.videodownloader.support.SupportManager;
import com.github.luischavez.videodownloader.task.*;
import org.pushingpixels.substance.api.skin.SubstanceNightShadeLookAndFeel;

import javax.swing.*;
import java.util.List;

public class Main {

    private final AppContext context;
    private final MainFrame mainFrame;

    public Main() {
        context = AppContext.instance();
        mainFrame = new MainFrame();
    }

    private void start() {
        context.getSystem().getDependencyInjection().configure(dependencyRegister -> {
            dependencyRegister.bindClass(Context.class).toInstance(context);

            dependencyRegister.bindClass(ConfigurationManager.class).toClass(SerializerConfigurationManager.class);
            dependencyRegister.bindClass(ScheduleManager.class).toClass(DefaultScheduleManager.class);
            dependencyRegister.bindClass(SupportManager.class).toClass(PluggableSupportManager.class);
            dependencyRegister.bindClass(TaskManager.class).toClass(DefaultTaskManager.class);
        });

        context.getSystem().registerManager(ConfigurationManager.class);
        context.getSystem().registerManager(ScheduleManager.class);
        context.getSystem().registerManager(SupportManager.class);
        context.getSystem().registerManager(TaskManager.class);

        context.getSystem().startAllManagers(true);

        final ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);
        final ScheduleManager scheduleManager = context.getSystem().getManager(ScheduleManager.class);

        final List<StreamConfiguration> streamConfigurations = configurationManager.list(StreamConfiguration.class);
        streamConfigurations.stream()
                .forEach(streamConfiguration -> {
                    final long uid = streamConfiguration.uid();

                    final ScheduleTask scheduleTask = new ScheduleStreamTask(context, uid);
                    final List<Schedule> schedules = streamConfiguration.getSchedules();

                    if (streamConfiguration.isEnabled() && streamConfiguration.isScheduleWhenAvailable()) {
                        scheduleManager.add(uid, new AllTimeSchedule(), scheduleTask);
                    } else if (streamConfiguration.isEnabled() && (schedules != null && !schedules.isEmpty())) {
                        scheduleManager.add(uid, new SchedulePicker(schedules), scheduleTask);
                    } else {
                        scheduleManager.add(uid, new NeverSchedule(), scheduleTask);
                    }
                });

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceNightShadeLookAndFeel());
            } catch (Exception ex) {
                // IGNORE
            }

            initGui();
        });
    }

    private void initGui() {
        MainFrame mainFrame = new MainFrame();
        mainFrame.initialize();

        JFrame.setDefaultLookAndFeelDecorated(true);

        GuiRepaintManager guiRepaintManager = context.getSystem().getManager(GuiRepaintManager.class);
        guiRepaintManager.addComponent(mainFrame.contentPanel.streamTable);
        guiRepaintManager.start();

        context.setGuiLogger((message) -> {
            if (mainFrame.logTextArea.getLineCount() > 100) {
                mainFrame.logTextArea.setText("");
            }

            mainFrame.logTextArea.append(message);
            mainFrame.logTextArea.append("\n");
            mainFrame.logTextArea.setCaretPosition(mainFrame.logTextArea.getDocument().getLength());
        });
    }

    public static void main(String... args) throws Exception {
        RunningPids.killAll();

        new Main().start();
    }
}
