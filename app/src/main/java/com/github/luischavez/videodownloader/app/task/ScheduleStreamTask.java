package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.schedule.KeepRunningScheduleTask;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduleStreamTask extends KeepRunningScheduleTask {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd_HH_mm_ss");
    private static final long FAST_FAILURE_THRESHOLD_MS = 15_000L;
    private static final long FIRST_RETRY_DELAY_MS = 30_000L;
    private static final long SECOND_RETRY_DELAY_MS = 60_000L;
    private static final long MAX_RETRY_DELAY_MS = 120_000L;
    private static final ScheduledExecutorService DAILY_ROTATION_EXECUTOR = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "DailyStreamRotation");
        thread.setDaemon(true);
        return thread;
    });

    private final long tag;

    private volatile StreamConfiguration streamConfiguration;
    private volatile Task task;
    private Task trackedTask;
    private boolean trackedTaskRunning;
    private long trackedTaskStartTime;
    private int consecutiveFastFailures;
    private long nextAllowedRetryTime;
    private ScheduledFuture<?> dailyRotationFuture;
    private Task dailyRotationTask;
    private String dailyRotationSignature;

    public ScheduleStreamTask(Context context, long tag) {
        super(context);

        this.tag = tag;

        refresh();
        scheduleDailyRotationIfNeeded();
    }

    public void refresh() {
        streamConfiguration = (StreamConfiguration) getSystem().getManager(ConfigurationManager.class).find(tag);
        task = getSystem().getManager(TaskManager.class).get(tag);
    }

    private boolean isYouTubeStream() {
        return streamConfiguration != null
                && streamConfiguration.getUrl() != null
                && streamConfiguration.getUrl().toLowerCase().contains("youtube.com");
    }

    private boolean usesPreferredVideoQuality() {
        if (streamConfiguration == null || streamConfiguration.getUrl() == null) {
            return false;
        }

        String location = streamConfiguration.getUrl().toLowerCase();
        return isYouTubeStream()
                || location.contains("cnnbrasil.com.br/ao-vivo")
                || location.contains("nbcnews.com/watch")
                || location.contains("cbsnews.com/");
    }

    private long resolveRetryDelay() {
        if (consecutiveFastFailures <= 1) {
            return FIRST_RETRY_DELAY_MS;
        }

        if (consecutiveFastFailures == 2) {
            return SECOND_RETRY_DELAY_MS;
        }

        return MAX_RETRY_DELAY_MS;
    }

    private void resetRetryState() {
        consecutiveFastFailures = 0;
        nextAllowedRetryTime = 0L;
    }

    private String buildDailyRotationSignature() {
        if (streamConfiguration == null) {
            return "";
        }

        return StreamTime.zone(streamConfiguration).getId()
                + "|" + streamConfiguration.getDailySplitAt();
    }

    private synchronized void cancelDailyRotation() {
        if (dailyRotationFuture != null) {
            dailyRotationFuture.cancel(false);
        }

        dailyRotationFuture = null;
        dailyRotationTask = null;
        dailyRotationSignature = null;
    }

    private synchronized void scheduleDailyRotationIfNeeded() {
        if (streamConfiguration == null || !streamConfiguration.isDailySplit()
                || task == null || !task.isRunning()) {
            cancelDailyRotation();
            return;
        }

        String signature = buildDailyRotationSignature();
        if (dailyRotationFuture != null && !dailyRotationFuture.isDone()
                && dailyRotationTask == task
                && signature.equals(dailyRotationSignature)) {
            return;
        }

        cancelDailyRotation();

        Instant now = Instant.now();
        ZonedDateTime nextSplit = StreamTime.nextDailySplit(streamConfiguration, now);
        long delay = StreamTime.millisUntilNextDailySplit(streamConfiguration, now);
        Task expectedTask = task;

        dailyRotationTask = expectedTask;
        dailyRotationSignature = signature;
        dailyRotationFuture = DAILY_ROTATION_EXECUTOR.schedule(
                () -> rotateAtDailyBoundary(expectedTask, signature),
                delay,
                TimeUnit.MILLISECONDS);

        debug(ScheduleStreamTask.class, String.format(
                "daily rotation scheduled tag=%d zone=%s split_at=%s next=%s",
                tag, StreamTime.zone(streamConfiguration).getId(),
                streamConfiguration.getDailySplitAt(), nextSplit));
    }

    private synchronized void rotateAtDailyBoundary(Task expectedTask, String expectedSignature) {
        dailyRotationFuture = null;
        dailyRotationTask = null;
        dailyRotationSignature = null;
        refresh();

        if (streamConfiguration == null || !streamConfiguration.isEnabled()
                || !streamConfiguration.isDailySplit()
                || !expectedSignature.equals(buildDailyRotationSignature())) {
            scheduleDailyRotationIfNeeded();
            return;
        }

        if (task != null && task != expectedTask) {
            scheduleDailyRotationIfNeeded();
            return;
        }

        debug(ScheduleStreamTask.class, String.format(
                "daily rotation started tag=%d alias=%s zone=%s",
                tag, streamConfiguration.getAlias(), StreamTime.zone(streamConfiguration).getId()));

        try {
            if (task == expectedTask) {
                getSystem().getManager(TaskManager.class).remove(tag);
            }
            task = null;
            trackedTask = null;
            trackedTaskRunning = false;
            trackedTaskStartTime = 0L;
            resetRetryState();
            generateTask();
        } catch (Exception ex) {
            error(ScheduleStreamTask.class,
                    String.format("daily rotation failed tag=%d alias=%s", tag, streamConfiguration.getAlias()),
                    ex);
        }
    }

    private void finalizeTrackedTask(long now) {
        if (!isYouTubeStream() || trackedTaskStartTime <= 0L) {
            trackedTaskStartTime = 0L;
            return;
        }

        long duration = now - trackedTaskStartTime;
        trackedTaskStartTime = 0L;

        if (duration < FAST_FAILURE_THRESHOLD_MS) {
            consecutiveFastFailures++;
            nextAllowedRetryTime = now + resolveRetryDelay();

            debug(ScheduleStreamTask.class, String.format(
                    "youtube retry cooldown tag=%d failures=%d duration_ms=%d retry_at=%d",
                    tag, consecutiveFastFailures, duration, nextAllowedRetryTime));
        } else {
            resetRetryState();
        }
    }

    private void updateTrackedTaskState(long now) {
        Task currentTask = task;
        boolean currentTaskRunning = currentTask != null && currentTask.isRunning();

        if (trackedTask != currentTask) {
            if (trackedTask != null && trackedTaskRunning) {
                finalizeTrackedTask(now);
            }

            trackedTask = currentTask;
            trackedTaskRunning = currentTaskRunning;
            trackedTaskStartTime = currentTaskRunning ? now : 0L;
            return;
        }

        if (!trackedTaskRunning && currentTaskRunning) {
            trackedTaskRunning = true;
            trackedTaskStartTime = now;
            return;
        }

        if (trackedTaskRunning && !currentTaskRunning) {
            trackedTaskRunning = false;
            finalizeTrackedTask(now);
        }
    }

    private synchronized void generateTask() throws Exception {
        refresh();
        long now = System.currentTimeMillis();

        updateTrackedTaskState(now);

        if (task != null) {
            if (task.isFresh() || task.isRunning()) {
                scheduleDailyRotationIfNeeded();
                return;
            }
        }

        if (isYouTubeStream() && nextAllowedRetryTime > now) {
            return;
        }

        Support support = getSystem().getManager(SupportManager.class).get(streamConfiguration.getUrl());
        List<Media> medias = getSystem().getManager(SupportManager.class).media(streamConfiguration.getUrl());

        if (!medias.isEmpty()) {
            final ZonedDateTime currentTime = StreamTime.now(streamConfiguration);

            String destinationPath = streamConfiguration.getDestinationPath();
            String alias = streamConfiguration.getAlias();
            destinationPath = buildPath(destinationPath, currentTime.format(DIRECTORY_FORMATTER), alias);

            File destinationFolderFile = new File(destinationPath);
            if (!destinationFolderFile.exists()) destinationFolderFile.mkdirs();

            if (streamConfiguration.getConcatenationPath() != null && !streamConfiguration.getConcatenationPath().isEmpty()) {
                File concatenationFolderFile = new File(streamConfiguration.getDestinationPath());

                if (!concatenationFolderFile.exists()) concatenationFolderFile.mkdirs();
            }

            // Intentional policy: after sorting, pick the lowest available quality.
            // If later you want the app to prefer a higher quality again, this is the
            // single place to change that behavior.
            medias.sort(Comparable::compareTo);
            Media selectedMedia = selectPreferredMedia(medias);

            String baseFileName = streamConfiguration.getBaseFileName() + "_" + currentTime.format(FILE_NAME_FORMATTER);

            Map<String, Object> taskParameters = new HashMap<>();
            taskParameters.put("base_file_name", baseFileName);
            taskParameters.put("destination_path", destinationPath);
            taskParameters.put("output_type", streamConfiguration.getType());

            if (streamConfiguration.isDailySplit()) {
                long millisUntilSplit = StreamTime.millisUntilNextDailySplit(
                        streamConfiguration, Instant.now());
                long secondsUntilSplit = Math.max(1L, (millisUntilSplit + 999L) / 1_000L);
                taskParameters.put("max_duration_seconds", secondsUntilSplit);
            }

            task = support.generateTask(streamConfiguration.getUrl(), selectedMedia, taskParameters);

            getSystem().getManager(TaskManager.class).add(tag, task);
            trackedTask = task;
            trackedTaskRunning = task.isRunning();
            trackedTaskStartTime = trackedTaskRunning ? System.currentTimeMillis() : 0L;
            scheduleDailyRotationIfNeeded();
        }
    }

    @Override
    protected void doTask() throws Exception {
        generateTask();
    }

    @Override
    protected void doDisable() throws Exception {
        cancelDailyRotation();
        if (task != null) {
            task.kill();
        }
    }

    private Media selectPreferredMedia(List<Media> medias) {
        if (medias == null || medias.isEmpty()) {
            return null;
        }

        if (usesPreferredVideoQuality()) {
            int preferredQuality = streamConfiguration != null ? streamConfiguration.getPreferredQuality() : 0;

            Media exactMedia = medias.stream()
                    .filter(media -> media instanceof Video && media.getQuality() instanceof Video.VideoQuality)
                    .filter(media -> Video.VideoQuality.class.cast(media.getQuality()).getHeight() == preferredQuality)
                    .findFirst()
                    .orElse(null);

            if (exactMedia != null) {
                return exactMedia;
            }

            Media preferredMedia = medias.stream()
                    .filter(media -> media instanceof Video && media.getQuality() instanceof Video.VideoQuality)
                    .filter(media -> Video.VideoQuality.class.cast(media.getQuality()).getHeight() >= preferredQuality)
                    .findFirst()
                    .orElse(null);

            if (preferredMedia != null) {
                return preferredMedia;
            }

            Media fallbackMedia = medias.stream()
                    .filter(media -> media instanceof Video && media.getQuality() instanceof Video.VideoQuality)
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (fallbackMedia != null) {
                return fallbackMedia;
            }
        }

        return medias.get(0);
    }

    @Override
    public boolean isRunning() {
        return task != null && task.isRunning();
    }
}
