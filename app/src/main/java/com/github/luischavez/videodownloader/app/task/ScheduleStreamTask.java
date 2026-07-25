package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.schedule.KeepRunningScheduleTask;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ScheduleStreamTask extends KeepRunningScheduleTask {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd_HH_mm_ss");
    private static final long FAST_FAILURE_THRESHOLD_MS = 15_000L;
    private static final long FIRST_RETRY_DELAY_MS = 30_000L;
    private static final long SECOND_RETRY_DELAY_MS = 60_000L;
    private static final long MAX_RETRY_DELAY_MS = 120_000L;

    private final long tag;

    private StreamConfiguration streamConfiguration;
    private Task task;
    private Task trackedTask;
    private boolean trackedTaskRunning;
    private long trackedTaskStartTime;
    private int consecutiveFastFailures;
    private long nextAllowedRetryTime;

    public ScheduleStreamTask(Context context, long tag) {
        super(context);

        this.tag = tag;

        refresh();
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

    private void generateTask() throws Exception {
        refresh();
        long now = System.currentTimeMillis();

        updateTrackedTaskState(now);

        if (task != null) {
            if (task.isFresh() || task.isRunning()) {
                return;
            }
        }

        if (isYouTubeStream() && nextAllowedRetryTime > now) {
            return;
        }

        Support support = getSystem().getManager(SupportManager.class).get(streamConfiguration.getUrl());
        List<Media> medias = getSystem().getManager(SupportManager.class).media(streamConfiguration.getUrl());

        if (!medias.isEmpty()) {
            final LocalDateTime currentTime = LocalDateTime.now();

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

            task = support.generateTask(streamConfiguration.getUrl(), selectedMedia,
                    Map.of("base_file_name", baseFileName,
                            "destination_path", destinationPath,
                            "output_type", streamConfiguration.getType()));

            getSystem().getManager(TaskManager.class).add(tag, task);
            trackedTask = task;
            trackedTaskRunning = task.isRunning();
            trackedTaskStartTime = trackedTaskRunning ? System.currentTimeMillis() : 0L;
        }
    }

    @Override
    protected void doTask() throws Exception {
        generateTask();
    }

    @Override
    protected void doDisable() throws Exception {
        task.kill();
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
