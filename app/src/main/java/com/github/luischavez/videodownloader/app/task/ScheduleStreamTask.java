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

    private final long tag;

    private StreamConfiguration streamConfiguration;
    private Task task;

    public ScheduleStreamTask(Context context, long tag) {
        super(context);

        this.tag = tag;

        streamConfiguration = (StreamConfiguration) getSystem().getManager(ConfigurationManager.class).find(tag);
        task = getSystem().getManager(TaskManager.class).get(tag);
    }

    private void generateTask() throws Exception {
        if (task != null) {
            if (task.isFresh() || task.isRunning()) {
                return;
            }
        }

        Support support = getSystem().getManager(SupportManager.class).get(streamConfiguration.getUrl());
        List<Media> medias = getSystem().getManager(SupportManager.class).media(streamConfiguration.getUrl());

        if (!medias.isEmpty()) {
            final LocalDateTime now = LocalDateTime.now();

            String destinationPath = streamConfiguration.getDestinationPath();
            destinationPath = buildPath(destinationPath, now.format(DIRECTORY_FORMATTER));

            File destinationFolderFile = new File(destinationPath);
            if (!destinationFolderFile.exists()) destinationFolderFile.mkdirs();

            if (streamConfiguration.getConcatenationPath() != null && !streamConfiguration.getConcatenationPath().isEmpty()) {
                File concatenationFolderFile = new File(streamConfiguration.getDestinationPath());

                if (!concatenationFolderFile.exists()) concatenationFolderFile.mkdirs();
            }

            medias.sort(Comparable::compareTo);

            Media selectedMedia = null;

            for (Media media : medias) {
                Quality quality = media.getQuality();

                if (quality instanceof Video.VideoQuality) {
                    if (Video.VideoQuality.class.cast(quality).getHeight() >= streamConfiguration.getPreferredQuality()) {
                        selectedMedia = media;
                        break;
                    }
                }
            }

            if (selectedMedia == null) selectedMedia = medias.get(medias.size() - 1);

            String baseFileName = streamConfiguration.getBaseFileName() + "_" + now.format(FILE_NAME_FORMATTER);

            task = support.generateTask(streamConfiguration.getUrl(), selectedMedia,
                    Map.of("base_file_name", baseFileName,
                            "destination_path", destinationPath));

            getSystem().getManager(TaskManager.class).add(tag, task);
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

    @Override
    public boolean isRunning() {
        return task != null && task.isRunning();
    }
}
