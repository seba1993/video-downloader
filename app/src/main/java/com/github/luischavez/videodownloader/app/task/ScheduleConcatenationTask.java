package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.schedule.SingleScheduleTask;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScheduleConcatenationTask extends SingleScheduleTask {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy'_'MM'_'dd_HH_mm_ss");

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^.+_(?<datetime>(?<year>[0-9]{4})_(?<month>[0-9]{2})_(?<day>[0-9]{2})_(?<hour>[0-9]{2})_(?<minute>[0-9]{2})_(?<second>[0-9]{2})).+$");

    private final long tag;

    private StreamConfiguration streamConfiguration;
    private Task task;

    public ScheduleConcatenationTask(Context context, long tag) {
        super(context);

        this.tag = tag;

        streamConfiguration = (StreamConfiguration) getSystem().getManager(ConfigurationManager.class).find(tag * -1);
        task = getSystem().getManager(TaskManager.class).get(tag);
    }

    private File[] getConcatenateFiles() {
        final LocalDateTime to = LocalDateTime.now().with(streamConfiguration.getConcatenateAt());
        final LocalDateTime from = to.minusDays(1);

        final String extension = streamConfiguration.getType().equals("Video") ? "mkv" : "mp3";
        final File sourceDirectoryFile = new File(streamConfiguration.getDestinationPath());

        final File[] directoryFiles = sourceDirectoryFile
                .listFiles((dir, name) -> dir.isDirectory() && (name.equals(from.format(DIRECTORY_FORMATTER)) || name.equals(to.format(DIRECTORY_FORMATTER))));

        final ArrayList<File> allFiles = new ArrayList<>();

        Arrays.asList(directoryFiles).stream()
                .forEach(file -> {
                    File[] files = file.listFiles();

                    if (files != null && files.length > 0) {
                        allFiles.addAll(Arrays.asList(files));
                    }
                });

        return allFiles.stream()
                .filter(file -> file.getName().toLowerCase().endsWith(extension))
                .filter(file -> {
                    if (file.length() < 2048) return false;

                    Matcher matcher = FILE_NAME_PATTERN.matcher(file.getName());

                    if (matcher.find()) {
                        String datetime = matcher.group("datetime");
                        LocalDateTime localDateTime = LocalDateTime.parse(datetime, FILE_NAME_FORMATTER);

                        return from.isBefore(localDateTime) && to.isAfter(localDateTime);
                    }

                    return false;
                })
                .collect(Collectors.toList())
                .toArray(new File[0]);
    }

    private File getDestinationFile(String extension) {
        final String baseFileName = streamConfiguration.getBaseFileName();
        final LocalDateTime now = LocalDateTime.now();

        final String fileName = String.format("%s_CONCAT_%s.%s", baseFileName, now.format(FILE_NAME_FORMATTER), extension);

        return new File(buildPath(streamConfiguration.getConcatenationPath(), fileName));
    }

    private void generateTask() throws Exception {
        if (task != null) {
            if (task.isFresh() || task.isRunning()) {
                return;
            }
        }

        final String extension = streamConfiguration.getType().equals("Video") ? "mkv" : "mp3";
        final File sources[] = getConcatenateFiles();
        final File destination = getDestinationFile(extension);
        final String language = streamConfiguration.getLanguage();

        ThreadedConcatenationTask.ConcatenationTaskBuilder builder = new ThreadedConcatenationTask.ConcatenationTaskBuilder(getWrappedContext());
        if (streamConfiguration.isConcatenate()) builder.concatenate();
        if (streamConfiguration.isSub()) builder.sub();

        builder
                .sources(sources)
                .destination(destination)
                .extension(extension)
                .language(language);

        builder
                .onInput(null)
                .onStart(() -> {
                    AppContext.instance().info(ScheduleConcatenationTask.class, String.format("concatenation started: %s", streamConfiguration.getAlias()));
                })
                .onStop(() -> {
                    AppContext.instance().info(ScheduleConcatenationTask.class, String.format("concatenation finished: %s", streamConfiguration.getAlias()));
                });

        task = builder.build();

        TaskManager taskManager = getSystem().getManager(TaskManager.class);

        Task downloadTask = taskManager.get(tag * -1);
        if (downloadTask != null) downloadTask.kill();

        taskManager.add(tag, this.task);
    }

    @Override
    protected void doTask() throws Exception {
        generateTask();
    }

    @Override
    protected void doDisable() throws Exception {
        // NEVER KILL BY SCHEDULE
        //task.kill();
    }

    @Override
    public boolean isRunning() {
        return task != null && task.isRunning();
    }
}
