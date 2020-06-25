package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.BaseContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.app.task.ScheduleStreamTask;
import com.github.luischavez.videodownloader.configuration.Configuration;
import com.github.luischavez.videodownloader.configuration.ConfigurationListener;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.Manager;
import com.github.luischavez.videodownloader.manager.ManagerListener;
import com.github.luischavez.videodownloader.schedule.*;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.support.Support;
import com.github.luischavez.videodownloader.support.SupportListener;
import com.github.luischavez.videodownloader.system.DefaultSystem;
import com.github.luischavez.videodownloader.system.GuiceDependencyInjection;
import com.github.luischavez.videodownloader.system.System;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskListener;
import com.github.luischavez.videodownloader.task.TaskStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

public class AppContext extends BaseContext implements ManagerListener,
        ConfigurationListener, ScheduleListener, SupportListener, TaskListener, TaskStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContext.class);

    private static AppContext CONTEXT_INSTANCE = null;

    private GuiUpdater guiUpdater;
    private GuiLogger guiLogger;

    private AppContext(System system) {
        super(system);
    }

    public void setGuiUpdater(GuiUpdater guiUpdater) {
        this.guiUpdater = guiUpdater;
    }

    public void setGuiLogger(GuiLogger guiLogger) {
        this.guiLogger = guiLogger;
    }

    private void triggerGuiUpdater() {
        if (guiUpdater == null) return;

        SwingUtilities.invokeLater(() -> guiUpdater.updateGui());
    }

    @Override
    public void log(Class<?> caller, String level, String message, Throwable cause) {
        if (guiLogger != null) guiLogger.log(message);

        if (cause != null) {
            LOGGER.error(message, cause);
            //cause.printStackTrace();
        }
    }

    @Override
    public void onManagerStart(Manager manager) {
        debug(AppContext.class, String.format("manager %s started", manager.getClass()));
    }

    @Override
    public void onManagerStop(Manager manager) {
        debug(AppContext.class, String.format("manager %s stopped", manager.getClass()));

        manager.start();
    }

    @Override
    public void onManagerExecutionException(Manager manager, String message, Throwable throwable) {
        error(AppContext.class, String.format("manager %s error %s %s", manager.getClass(), message, throwable.getMessage()), throwable);
    }

    @Override
    public void onConfigurationChange(Configuration configuration) {
        debug(AppContext.class, String.format("configuration %s %d changed", configuration.getClass(), configuration.uid()));

        if (configuration instanceof StreamConfiguration) {
            ScheduleManager scheduleManager = getSystem().getManager(ScheduleManager.class);
            StreamConfiguration streamConfiguration = StreamConfiguration.class.cast(configuration);

            long uid = streamConfiguration.uid();

            getSystem().getManager(ScheduleManager.class).remove(uid);

            if (getSystem().getManager(ConfigurationManager.class).find(configuration.uid()) == null) {
                return;
            }

            final ScheduleTask scheduleTask = new ScheduleStreamTask(this, uid);
            final List<Schedule> schedules = streamConfiguration.getSchedules();

            if (streamConfiguration.isEnabled() && streamConfiguration.isScheduleWhenAvailable()) {
                scheduleManager.add(uid, new AllTimeSchedule(), scheduleTask);
            } else if (streamConfiguration.isEnabled() && (schedules != null && !schedules.isEmpty())) {
                scheduleManager.add(uid, new SchedulePicker(schedules), scheduleTask);
            } else {
                scheduleManager.add(uid, new NeverSchedule(), scheduleTask);
            }

            triggerGuiUpdater();
        }
    }

    @Override
    public void onScheduleTask(Schedule schedule, ScheduleTask task) {
        //debug(AppContext.class, String.format("schedule task %s", task.getClass()));
        triggerGuiUpdater();
    }

    @Override
    public void onScheduleDisabled(Schedule schedule, ScheduleTask task) {
        //debug(AppContext.class, String.format("schedule %s disabled", task.getClass()));
        triggerGuiUpdater();
    }

    @Override
    public void onScheduleException(Schedule schedule, ScheduleTask task, Throwable throwable) {
        error(AppContext.class, String.format("schedule %s exception %s", task.getClass(), throwable.getMessage()), throwable);
        triggerGuiUpdater();
    }

    @Override
    public void onNewSupport(Support support) {
        debug(AppContext.class, String.format("new support detected %s", support.getClass()));
    }

    @Override
    public void onSupportNotFound(String location) {
        error(AppContext.class, String.format("support not found for location %s", location));
    }

    @Override
    public void onMediaFound(String location, List<? extends Media> medias) {
        debug(AppContext.class, String.format("%d media files found for location %s", medias.size(), location));
    }

    @Override
    public void onMediaNotFound(String location) {
        error(AppContext.class, String.format("media not found for location %s", location));
    }

    @Override
    public void onMediaOffline(String location, Throwable throwable) {
        error(AppContext.class, String.format("media location offline %s", location), throwable);
    }

    @Override
    public void onTaskAdded(Task task) {
        debug(AppContext.class, String.format("task added %s", task.getClass()));
        triggerGuiUpdater();
    }

    @Override
    public void onTaskRemoved(Task task) {
        debug(AppContext.class, String.format("task removed %s", task.getClass()));
        triggerGuiUpdater();
    }

    @Override
    public void onTaskStart(Task task) {
        debug(AppContext.class, String.format("task started %s pid %d", task.getClass(), task.pid()));
        triggerGuiUpdater();
        RunningPids.load().add(task.pid());
    }

    @Override
    public void onTaskStop(Task task) {
        debug(AppContext.class, String.format("task stopped %s pid %d", task.getClass(), task.pid()));
        triggerGuiUpdater();
        RunningPids.load().remove(task.pid());
    }

    @Override
    public void onTaskException(Task task, Throwable throwable) {
        error(AppContext.class, String.format("task exception %s %s", task.getClass(), throwable.getMessage()), throwable);
    }

    public static AppContext instance() {
        if (CONTEXT_INSTANCE == null) CONTEXT_INSTANCE = new AppContext(new DefaultSystem(new GuiceDependencyInjection()));

        return CONTEXT_INSTANCE;
    }

    @FunctionalInterface
    interface GuiUpdater {

        void updateGui();
    }

    interface GuiLogger {

        void log(String message);
    }
}
