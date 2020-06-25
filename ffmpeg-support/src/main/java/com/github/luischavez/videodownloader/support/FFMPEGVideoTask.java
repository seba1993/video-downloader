package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.LocalProcessTask;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FFMPEGVideoTask extends LocalProcessTask {

    private final Media media;
    private final String baseFileName;
    private final String destinationPath;
    private final String[] headers;

    public FFMPEGVideoTask(Context context, Media media,
                           String baseFileName, String destinationPath,
                           String... headers) {
        super(context);

        this.media = media;

        this.baseFileName = baseFileName;
        this.destinationPath = destinationPath;

        this.headers = headers;
    }

    @Override
    public String details() {
        return media.getQuality().toString();
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        final boolean isVideo = media instanceof Video;
        final String url = media.getUrl();

        final String fileExtension = isVideo ? "mkv" : "mp3";
        final String fileName = String.format("%s.%s", baseFileName, fileExtension);
        final String filePath = buildPath(destinationPath, fileName);

        File folderFile = new File(destinationPath);

        if (!folderFile.exists()) Files.createDirectories(folderFile.toPath());

        String headers = Arrays.asList(this.headers).stream()
                .map(header -> String.format("-headers \"%s\"", header))
                .collect(Collectors.joining(" "));

        String subCommand;
        if (isVideo) {
            subCommand = String.format("ffmpeg -nostdin -xerror -abort_on empty_output %s -i \"%s\" -c copy \"%s\"", headers, url, filePath);
        } else {
            subCommand = String.format("ffmpeg -nostdin -xerror -abort_on empty_output %s -i \"%s\" -vn -f mp3 \"%s\"", headers, url, filePath);
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(PlatformUtils.isWindowsHost() ? "cmd.exe" : "sh");
        command.add(PlatformUtils.isWindowsHost() ? "/c" : "-c");
        command.add(subCommand);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command.toArray(new String[0]));

        return processBuilder;
    }
}
