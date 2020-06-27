package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.util.FormatUtils;
import com.github.luischavez.videodownloader.task.LocalProcessTask;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.File;

public class ClipTask extends LocalProcessTask {

    private final long id;

    private final File file;
    private final long startAt;
    private final long stopAt;
    private final long length;

    private String status;

    private File directory;

    private int secondsBefore;
    private int secondsAfter;

    public ClipTask(Context context, File file, long startAt, long stopAt, long length) {
        super(context);

        id = System.nanoTime();

        this.file = file;
        this.startAt = startAt;
        this.stopAt = stopAt;
        this.length = length;

        status = "not started";

        secondsBefore = 0;
        secondsAfter = 0;
    }

    public long getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public long getStartAt() {
        if (startAt - (secondsBefore * 1_000L) < 0) {
            return 0;
        }

        return startAt - (secondsBefore * 1_000L);
    }

    public long getStopAt() {
        if (stopAt + (secondsAfter * 1_000L) > length) {
            return length;
        }

        return stopAt + (secondsAfter * 1_000L);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setTimeSpan(int secondsBefore, int secondsAfter) {
        this.secondsBefore = secondsBefore;
        this.secondsAfter = secondsAfter;
    }

    private long getStartSeconds() {
        return getStartAt() / 1_000L;
    }

    private long getStopSeconds() {
        return getStopAt() / 1_000L;
    }

    private String getFileName() {
        final String fileNameFull = file.getName();
        final String startTimeString = FormatUtils.formatTime(getStartAt());
        final String stopTimeString = FormatUtils.formatTime(getStopAt());

        final String[] fileNameParts = fileNameFull.split("\\.");
        final String fileName = fileNameParts[0];
        final String extension = fileNameParts.length > 1 ? fileNameParts[1] : "";

        return String.format("%s_%s-%s.%s",
                fileName,
                startTimeString.replace(":", "_"),
                stopTimeString.replace(":", "_"),
                extension);
    }

    @Override
    public String details() {
        return String.format("%s from: %s to: %s length: %s",
                file.getName(),
                FormatUtils.formatTime(getStartAt()), FormatUtils.formatTime(getStopAt()),
                FormatUtils.formatTime(getStartAt() - getStopAt()));
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        status = "started";

        if (!directory.exists()) directory.mkdirs();

        final String fileName = getFileName();

        final File destinationFile = new File(directory, fileName);
        final long startSecond = getStartSeconds();
        final long stopSecond = getStopSeconds();

        if (destinationFile.exists()) destinationFile.delete();

        String subCommand = String.format("ffmpeg -ss %d -to %d -i \"%s\" -c copy \"%s\"",
                startSecond, stopSecond,
                file.getPath(), destinationFile.getPath());

        if (PlatformUtils.isWindowsHost()) {
            return new ProcessBuilder().command("cmd.exe", "/c", subCommand);
        }

        return new ProcessBuilder().command("sh", "-c", subCommand);
    }
}
