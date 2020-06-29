package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.LocalProcessTask;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

public class FFMPEGTask extends LocalProcessTask {

    private final Media media;
    private final String command;
    private final String fileName;
    private final String destinationPath;

    public FFMPEGTask(Context context, Media media,
                      String command,
                      String fileName, String destinationPath) {
        super(context);

        this.media = media;

        this.command = command;

        this.fileName = fileName;
        this.destinationPath = destinationPath;
    }

    public Media getMedia() {
        return media;
    }

    public String getCommand() {
        return command;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    @Override
    public String details() {
        return media.getQuality().toString();
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        File folderFile = new File(getDestinationPath());
        if (!folderFile.exists()) Files.createDirectories(folderFile.toPath());

        ArrayList<String> command = new ArrayList<>();
        command.add(PlatformUtils.isWindowsHost() ? "cmd.exe" : "sh");
        command.add(PlatformUtils.isWindowsHost() ? "/c" : "-c");
        command.add(String.format("%s", getCommand()));

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command.toArray(new String[0]));

        return processBuilder;
    }
}
