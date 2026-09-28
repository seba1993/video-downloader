package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.gui.MainFrame;
import com.github.luischavez.videodownloader.app.manager.GuiRepaintManager;
import com.github.luischavez.videodownloader.app.manager.MonitorManager;
import com.github.luischavez.videodownloader.app.manager.YouTubeManager;
import com.github.luischavez.videodownloader.app.remote.RemoteApiServer;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.BaseManager;
import com.github.luischavez.videodownloader.manager.configuration.SerializerConfigurationManager;
import com.github.luischavez.videodownloader.manager.support.PluggableSupportManager;
import com.github.luischavez.videodownloader.schedule.DefaultScheduleManager;
import com.github.luischavez.videodownloader.schedule.ScheduleManager;
import com.github.luischavez.videodownloader.support.SupportManager;
import com.github.luischavez.videodownloader.task.DefaultTaskManager;
import com.github.luischavez.videodownloader.task.TaskManager;
import org.pushingpixels.substance.api.skin.SubstanceNightShadeLookAndFeel;

import javax.swing.*;
import java.util.List;

public class Downloader {

    private final AppContext context;

    private MainFrame mainFrame;

    private RemoteApiServer remoteApiServer;

    public Downloader() {
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
        context.getSystem().registerManager(MonitorManager.class);
        context.getSystem().registerManager(YouTubeManager.class);

        context.getSystem().startAllManagers(true);

        final ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);
        final ScheduleManager scheduleManager = context.getSystem().getManager(ScheduleManager.class);

        ((BaseManager) scheduleManager).setExecutionInterval(20_000L);

        final List<StreamConfiguration> streamConfigurations = configurationManager.list(StreamConfiguration.class);
        streamConfigurations.stream().forEach(context::updateSchedules);

        remoteApiServer = RemoteApiServer.startIfConfigured(context);
        if (remoteApiServer != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(remoteApiServer::stop, "RemoteApiShutdown"));
        }

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
        mainFrame = new MainFrame();
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

        context.getSystem().getManager(MonitorManager.class).setMainFrame(mainFrame);
    }

    public static void main(String... args) throws Exception {
        RunningPids.killAll();

        new Downloader().start();
    }
}
