package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.AppConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.task.ThreadProcessTask;
import com.github.luischavez.videodownloader.util.LanguageUtils;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThreadedConcatenationTask extends ThreadProcessTask {

    private boolean concatenate;
    private boolean sub;

    private String extension;

    private File[] sources;
    private File destination;

    private String language;

    private Consumer<String> inputConsumer;
    private Runnable onStart;
    private Runnable onStop;

    private final List<File> filesToClean;

    private ThreadedConcatenationTask(Context context) {
        super(context);
        filesToClean = new ArrayList<>();
    }

    public boolean isConcatenate() {
        return concatenate;
    }

    private void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;
    }

    public boolean isSub() {
        return sub;
    }

    private void setSub(boolean sub) {
        this.sub = sub;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public File[] getSources() {
        return sources;
    }

    private void setSources(File[] sources) {
        this.sources = sources;
    }

    public File getDestination() {
        return destination;
    }

    private void setDestination(File destination) {
        this.destination = destination;
    }

    public String getLanguage() {
        return language;
    }

    private void setLanguage(String language) {
        this.language = language;
    }

    public void setInputConsumer(Consumer<String> inputConsumer) {
        this.inputConsumer = inputConsumer;
    }

    public void setOnStart(Runnable onStart) {
        this.onStart = onStart;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    private void makeDirectories() throws Exception {
        if (!destination.getParentFile().exists()) destination.getParentFile().mkdirs();
    }

    private File createConcatenationListFile() throws Exception {
        File concatenationListFile = new File(destination.getParentFile(), String.format("concat_%d.txt", System.nanoTime()));
        if (concatenationListFile.exists()) concatenationListFile.delete();
        filesToClean.add(concatenationListFile);

        List<String> listContent = Arrays.asList(sources).stream()
                .map(file -> String.format("file '%s'", file.getPath()))
                .collect(Collectors.toList());

        Files.write(concatenationListFile.toPath(), listContent);

        return concatenationListFile;
    }

    private String buildConcatenateCommand() throws Exception {
        File concatenationListFile = createConcatenationListFile();

        return String.format("ffmpeg -f concat -safe 0 -i \"%s\" -c copy \"%s\"",
                concatenationListFile.getPath(), destination.getPath());
    }

    private String buildSubCommand() throws Exception {
        AppConfiguration appConfiguration = AppContext.instance().getSystem().getManager(ConfigurationManager.class).get(AppConfiguration.class);

        String languageCode = LanguageUtils.code(language);

        String source = concatenate
                ? destination.getPath()
                : sources[0].getPath();

        String output = concatenate
                ? destination.getPath().replace("." + extension, ".srt")
                : destination.getPath();

        return String.format("\"%s\" -S %s -D %s \"%s\" -o \"%s\"",
                appConfiguration.getAutosubPath(), languageCode, languageCode,
                source, output);
    }

    @Override
    public String details() {
        return "";
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        makeDirectories();

        ArrayList<String> commands = new ArrayList<>();

        if (concatenate) commands.add(buildConcatenateCommand());
        if (sub) commands.add(buildSubCommand());

        String subCommand = commands.stream().collect(Collectors.joining(" && "));

        if (PlatformUtils.isWindowsHost()) {
            return new ProcessBuilder().command("cmd.exe", "/c", subCommand);
        }

        return new ProcessBuilder().command("sh", "-c", subCommand);
    }

    @Override
    protected void onStart() {
        if (onStart != null) onStart.run();
    }

    @Override
    protected void onStop() {
        filesToClean.stream().forEach(file -> file.delete());
        if (onStop != null) onStop.run();
    }

    @Override
    protected void onInput(String line) {
        if (inputConsumer != null) inputConsumer.accept(line);
    }

    public static class ConcatenationTaskBuilder {

        private ThreadedConcatenationTask threadedConcatenationTask;

        public ConcatenationTaskBuilder(Context context) {
            threadedConcatenationTask = new ThreadedConcatenationTask(context);
        }

        public ConcatenationTaskBuilder concatenate() {
            threadedConcatenationTask.setConcatenate(true);
            return this;
        }

        public ConcatenationTaskBuilder sub() {
            threadedConcatenationTask.setSub(true);
            return this;
        }

        public ConcatenationTaskBuilder extension(String extension) {
            threadedConcatenationTask.setExtension(extension);
            return this;
        }

        public ConcatenationTaskBuilder sources(File... sources) {
            threadedConcatenationTask.setSources(sources);
            return this;
        }

        public ConcatenationTaskBuilder destination(File destination) {
            threadedConcatenationTask.setDestination(destination);
            return this;
        }

        public ConcatenationTaskBuilder language(String language) {
            threadedConcatenationTask.setLanguage(language);
            return this;
        }

        public ConcatenationTaskBuilder onInput(Consumer<String> inputConsumer) {
            threadedConcatenationTask.setInputConsumer(inputConsumer);
            return this;
        }

        public ConcatenationTaskBuilder onStart(Runnable runnable) {
            threadedConcatenationTask.setOnStart(runnable);
            return this;
        }

        public ConcatenationTaskBuilder onStop(Runnable runnable) {
            threadedConcatenationTask.setOnStop(runnable);
            return this;
        }

        public ThreadedConcatenationTask build() {
            return threadedConcatenationTask;
        }
    }
}
