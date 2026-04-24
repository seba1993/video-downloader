package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.BaseContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.mail.MailSender;
import com.github.luischavez.videodownloader.app.task.RunningPids;
import com.github.luischavez.videodownloader.app.task.ScheduleConcatenationTask;
import com.github.luischavez.videodownloader.app.task.ScheduleStreamTask;
import com.github.luischavez.videodownloader.app.task.ThreadedConcatenationTask;
import com.github.luischavez.videodownloader.configuration.Configuration;
import com.github.luischavez.videodownloader.configuration.ConfigurationListener;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.Manager;
import com.github.luischavez.videodownloader.manager.ManagerListener;
import com.github.luischavez.videodownloader.schedule.*;
import com.github.luischavez.videodownloader.support.FFMPEGTask;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.support.Support;
import com.github.luischavez.videodownloader.support.SupportListener;
import com.github.luischavez.videodownloader.system.DefaultSystem;
import com.github.luischavez.videodownloader.system.GuiceDependencyInjection;
import com.github.luischavez.videodownloader.system.System;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskListener;
import com.github.luischavez.videodownloader.task.TaskManager;
import com.github.luischavez.videodownloader.task.TaskStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AppContext extends BaseContext implements ManagerListener,
        ConfigurationListener, ScheduleListener, SupportListener, TaskListener, TaskStateListener {

    public static final AtomicBoolean exiting = new AtomicBoolean(false);

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContext.class);

    private static AppContext CONTEXT_INSTANCE = null;

    private GuiLogger guiLogger;

    private final Map<String, Long> lastMediaMailTimestamps;

    private AppContext(System system) {
        super(system);

        lastMediaMailTimestamps = new HashMap<>();
    }

    public void setGuiLogger(GuiLogger guiLogger) {
        this.guiLogger = guiLogger;
    }

    private void updateStreamSchedule(StreamConfiguration streamConfiguration) {
        final ConfigurationManager configurationManager = getSystem().getManager(ConfigurationManager.class);
        final ScheduleManager scheduleManager = getSystem().getManager(ScheduleManager.class);

        long uid = streamConfiguration.uid();

        scheduleManager.remove(uid);

        if (configurationManager.find(streamConfiguration.uid()) == null) return;

        final ScheduleTask scheduleTask = new ScheduleStreamTask(this, uid);
        final List<Schedule> schedules = streamConfiguration.getSchedules();

        if (streamConfiguration.isEnabled() && streamConfiguration.isScheduleWhenAvailable()) {
            scheduleManager.add(uid, new AllTimeSchedule(), scheduleTask);
        } else if (streamConfiguration.isEnabled() && (schedules != null && !schedules.isEmpty())) {
            scheduleManager.add(uid, new SchedulePicker(schedules), scheduleTask);
        } else {
            scheduleManager.add(uid, new NeverSchedule(), scheduleTask);
        }
    }

    private void updateConcatenateSchedule(StreamConfiguration streamConfiguration) {
        final ConfigurationManager configurationManager = getSystem().getManager(ConfigurationManager.class);
        final ScheduleManager scheduleManager = getSystem().getManager(ScheduleManager.class);

        long uid = streamConfiguration.uid() * -1;

        scheduleManager.remove(uid);

        if (configurationManager.find(streamConfiguration.uid()) == null) return;

        if (!streamConfiguration.isConcatenate()) return;

        final ScheduleTask scheduleTask = new ScheduleConcatenationTask(this, uid);

        scheduleManager.add(uid,
                new Schedule(Schedule.Day.Everyday, streamConfiguration.getConcatenateAt(), Schedule.ONE_MINUTE * 2),
                scheduleTask);
    }

    public void updateSchedules(StreamConfiguration streamConfiguration) {
        updateStreamSchedule(streamConfiguration);
        updateConcatenateSchedule(streamConfiguration);

        if (!streamConfiguration.isEnabled() || (!streamConfiguration.isScheduleWhenAvailable() && streamConfiguration.getSchedules().isEmpty())) {
            final TaskManager taskManager = getSystem().getManager(TaskManager.class);
            final Task task = taskManager.get(streamConfiguration.uid());

            if (task != null) {
                taskManager.remove(streamConfiguration.uid());
            }
        }
    }

    @Override
    public void log(Class<?> caller, String level, String message, Throwable cause) {
        if (guiLogger != null) guiLogger.log(message);

        if (cause != null) {
            LOGGER.error(message, cause);
            cause.printStackTrace();
        } else if (level.equals("error")) {
            LOGGER.error(message);
        }
    }

    @Override
    public void onManagerStart(Manager manager) {
        debug(AppContext.class, String.format("manager %s started", manager.getClass()));
    }

    @Override
    public void onManagerStop(Manager manager) {
        debug(AppContext.class, String.format("manager %s stopped", manager.getClass()));

        if (!exiting.get()) manager.start();
    }

    @Override
    public void onManagerExecutionException(Manager manager, String message, Throwable throwable) {
        error(AppContext.class, String.format("manager %s error %s %s", manager.getClass(), message, throwable.getMessage()), throwable);
    }

    @Override
    public void onConfigurationChange(Configuration configuration) {
        debug(AppContext.class, String.format("configuration %s %d changed", configuration.getClass(), configuration.uid()));

        if (configuration instanceof StreamConfiguration) {
            StreamConfiguration streamConfiguration = StreamConfiguration.class.cast(configuration);
            updateSchedules(streamConfiguration);
        }
    }

    @Override
    public void onScheduleTask(Schedule schedule, ScheduleTask task) {
        //debug(AppContext.class, String.format("schedule task %s", task.getClass()));
    }

    @Override
    public void onScheduleDisabled(Schedule schedule, ScheduleTask task) {
        //debug(AppContext.class, String.format("schedule %s disabled", task.getClass()));
    }

    @Override
    public void onScheduleException(Schedule schedule, ScheduleTask task, Throwable throwable) {
        error(AppContext.class, String.format("schedule %s exception %s", task.getClass(), throwable.getMessage()), throwable);
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

        final long timestamp = lastMediaMailTimestamps.getOrDefault(location, 0L);
        final long now = java.lang.System.currentTimeMillis();

        if (timestamp == 0 || (now - timestamp) >= (1_000L * 60 * 5)) {
            Map<String, Object> params = Map.of("url", location);

            final String subject = String.format("Media not found: %s", location);
            final String body = MailSender.buildFromTemplate(this,"media_not_found.html", params, subject);

            MailSender.send(this, subject, body);

            lastMediaMailTimestamps.put(location, now);
        }
    }

    @Override
    public void onMediaOffline(String location, Throwable throwable) {
        error(AppContext.class, String.format("media location offline %s", location), throwable);

        final long timestamp = lastMediaMailTimestamps.getOrDefault(location, 0L);
        final long now = java.lang.System.currentTimeMillis();

        if (timestamp == 0 || (now - timestamp) >= (1_000L * 60 * 5)) {
            Map<String, Object> params = Map.of("url", location);

            final String subject = String.format("Media offline: %s", location);
            final String body = MailSender.buildFromTemplate(this,"media_offline.html", params, subject);

            MailSender.send(this, subject, body);

            lastMediaMailTimestamps.put(location, now);
        }
    }

    @Override
    public void onTaskAdded(Task task) {
        debug(AppContext.class, String.format("task added %s", task.getClass()));

        if (task instanceof FFMPEGTask) {
            debug(AppContext.class, ((FFMPEGTask) task).getCommand());
        }
    }

    @Override
    public void onTaskRemoved(Task task) {
        debug(AppContext.class, String.format("task removed %s", task.getClass()));
    }

    private void sendTaskDetailMail(FFMPEGTask videoTask, boolean started) {
        final Media media = videoTask.getMedia();
        final String fileName = videoTask.getFileName();
        final String destinationPath = videoTask.getDestinationPath();

        Map<String, Object> params = Map.of("url", media.getUrl(),
                "quality", media.getQuality(),
                "file", fileName,
                "destination", destinationPath);

        final String subject = String.format("Task %s: %s", started ? "started" : "stopped", media.getUrl());
        final String body = MailSender.buildFromTemplate(this,"task_detail.html", params, subject);

        MailSender.send(this, subject, body);
    }

    private void sendConcatenationTaskMail(ThreadedConcatenationTask concatenationTask, boolean started) {
        final File[] sources = concatenationTask.getSources();
        final File destination = concatenationTask.getDestination();
        final String language = concatenationTask.getLanguage();
        final boolean sub = concatenationTask.isSub();

        Map<String, Object> params = Map.of(
                "sources", Arrays.asList(sources).stream().map(File::getName).collect(Collectors.joining("<br>")),
                "destination", destination.getPath(),
                "sub", sub,
                "language", language);

        final String subject = String.format("Concatenation Task %s", started ? "started" : "stopped");
        final String body = MailSender.buildFromTemplate(this,"concatenation_task_detail.html", params, subject);

        MailSender.send(this, subject, body);
    }

    @Override
    public void onTaskStart(Task task) {
        debug(AppContext.class, String.format("task started %s pid %d", task.getClass(), task.pid()));
        RunningPids.load().add(task.pid());

        if (task instanceof FFMPEGTask) {
            sendTaskDetailMail(FFMPEGTask.class.cast(task), true);
        } else if (task instanceof ThreadedConcatenationTask) {
            sendConcatenationTaskMail(ThreadedConcatenationTask.class.cast(task), true);
        }
    }

    @Override
    public void onTaskStop(Task task) {
        debug(AppContext.class, String.format("task stopped %s pid %d", task.getClass(), task.pid()));
        RunningPids.load().remove(task.pid());

        if (task instanceof FFMPEGTask) {
            sendTaskDetailMail(FFMPEGTask.class.cast(task), false);
        } else if (task instanceof ThreadedConcatenationTask) {
            sendConcatenationTaskMail(ThreadedConcatenationTask.class.cast(task), false);
        }
    }

    @Override
    public void onTaskException(Task task, Throwable throwable) {
        error(AppContext.class, String.format("task exception %s %s", task.getClass(), throwable.getMessage()), throwable);
    }

    public static AppContext instance() {
        if (CONTEXT_INSTANCE == null) CONTEXT_INSTANCE = new AppContext(new DefaultSystem(new GuiceDependencyInjection()));

        return CONTEXT_INSTANCE;
    }

    interface GuiLogger {

        void log(String message);
    }
}
