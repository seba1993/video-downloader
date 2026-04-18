package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.LocalProcessTask;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.File;

public class YouTubeTask extends LocalProcessTask {

    private static final String YT_DLP_EXECUTABLE = "yt-dlp";
    private static final String YOUTUBE_DL_EXECUTABLE = "youtube-dl";
    private static final String YT_DLP_CONFIG = "yt-dlp.conf";
    private static final String YOUTUBE_DL_CONFIG = "youtube-dl.conf";

    public YouTubeTask(Context context) {
        super(context);
    }

    @Override
    public String details() {
        return "yt-dlp downloader running: " + isRunning();
    }

    private File resolveConfigFile(String binDirectory) {
        final File ytDlpConfig = new File(getWrappedContext().buildPath(binDirectory, YT_DLP_CONFIG));
        if (ytDlpConfig.exists()) return ytDlpConfig;

        final File youtubeDlConfig = new File(getWrappedContext().buildPath(binDirectory, YOUTUBE_DL_CONFIG));
        if (youtubeDlConfig.exists()) return youtubeDlConfig;

        return ytDlpConfig;
    }

    private String resolveExecutable(String binDirectory) {
        final String extension = PlatformUtils.isWindowsHost() ? ".exe" : "";
        final File ytDlpExecutable = new File(getWrappedContext().buildPath(binDirectory, YT_DLP_EXECUTABLE + extension));
        if (ytDlpExecutable.exists()) return YT_DLP_EXECUTABLE + extension;

        final File youtubeDlExecutable = new File(getWrappedContext().buildPath(binDirectory, YOUTUBE_DL_EXECUTABLE + extension));
        if (youtubeDlExecutable.exists()) return YOUTUBE_DL_EXECUTABLE + extension;

        return YT_DLP_EXECUTABLE + extension;
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        String workingDirectory = System.getProperty("user.dir");
        String binDirectory = getWrappedContext().buildPath(workingDirectory, "youtube");
        final File configFile = resolveConfigFile(binDirectory);
        final String configPath = configFile.getPath();

        if (!configFile.exists()) {
            throw new RuntimeException(String.format("%s not exists!", configPath));
        }

        final String executable = resolveExecutable(binDirectory);
        String subCommand = String.format("\"%s\" --cookies-from-browser firefox --config-location \"%s\"", executable, configPath);

        if (PlatformUtils.isWindowsHost()) {
            return new ProcessBuilder()
                    .command("cmd.exe", "/c", subCommand)
                    .directory(new File(binDirectory));
        }

        return new ProcessBuilder()
                .command("sh", "-c", subCommand)
                .directory(new File(binDirectory));
    }
}
