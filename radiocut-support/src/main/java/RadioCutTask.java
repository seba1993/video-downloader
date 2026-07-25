import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskStateListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadioCutTask extends ContextWrapper implements Task {

    private static final long DIRECTORY_SECONDS = 10_000L;
    private static final long POLL_INTERVAL_MS = 15_000L;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/138 Safari/537.36";

    private final RadioCutMedia media;
    private final String destinationPath;
    private final String outputFileName;
    private final AtomicBoolean fresh;
    private final AtomicBoolean running;
    private final AtomicBoolean stopNotified;
    private final Set<String> downloadedChunks;

    private volatile Thread worker;
    private volatile double lastChunkStart;
    private volatile double cursorTimestamp;

    public RadioCutTask(Context context, RadioCutMedia media,
                        String destinationPath, String outputFileName) {
        super(context);

        this.media = media;
        this.destinationPath = destinationPath;
        this.outputFileName = outputFileName;
        this.fresh = new AtomicBoolean(true);
        this.running = new AtomicBoolean(false);
        this.stopNotified = new AtomicBoolean(false);
        this.downloadedChunks = new HashSet<>();
        this.lastChunkStart = -1D;
        this.cursorTimestamp = media.getStartTimestamp();
    }

    @Override
    public long pid() {
        return -1L;
    }

    @Override
    public String details() {
        return "RadioCut MP3 " + media.getStation();
    }

    @Override
    public boolean isFresh() {
        return fresh.get();
    }

    @Override
    public boolean isRunning() {
        return running.get() && worker != null && worker.isAlive();
    }

    @Override
    public synchronized void start() throws Exception {
        if (isRunning() || !fresh.compareAndSet(true, false)) {
            return;
        }

        File destination = new File(destinationPath);
        Files.createDirectories(destination.toPath());

        running.set(true);
        worker = new Thread(this::record, "RadioCut-" + media.getStation());
        worker.setDaemon(true);
        worker.start();

        getListeners(TaskStateListener.class).stream()
                .forEach(listener -> listener.onTaskStart(this));
    }

    @Override
    public synchronized void kill() throws Exception {
        boolean wasRunning = running.getAndSet(false);
        Thread currentWorker = worker;

        if (currentWorker != null) {
            currentWorker.interrupt();
            if (currentWorker != Thread.currentThread()) {
                currentWorker.join(5_000L);
            }
        }

        if (wasRunning || currentWorker != null) {
            notifyStopped();
        }
    }

    private void record() {
        File outputFile = new File(destinationPath, outputFileName);

        try (FileOutputStream output = new FileOutputStream(outputFile, false)) {
            while (running.get()) {
                try {
                    boolean appended = appendAvailableChunks(output);
                    if (appended) {
                        output.flush();
                    }
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Exception ex) {
                    error(RadioCutTask.class, "RadioCut poll failed; retrying " + media.getUrl(), ex);
                }

                sleepUntilNextPoll();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            error(RadioCutTask.class, "RadioCut recording failed for " + media.getUrl(), ex);
        } finally {
            running.set(false);
            notifyStopped();
        }
    }

    private void notifyStopped() {
        if (!stopNotified.compareAndSet(false, true)) {
            return;
        }

        getListeners(TaskStateListener.class).stream()
                .forEach(listener -> listener.onTaskStop(this));
    }

    private boolean appendAvailableChunks(FileOutputStream output) throws Exception {
        List<Chunk> chunks = new ArrayList<>();

        for (long directory : directoriesToPoll()) {
            chunks.addAll(getChunks(directory));
        }

        chunks.sort(Comparator.comparingDouble(chunk -> chunk.start));

        boolean appended = false;
        for (Chunk chunk : chunks) {
            if (!shouldAppend(chunk)) {
                continue;
            }

            appendChunk(chunk, output);
            downloadedChunks.add(chunk.key());
            lastChunkStart = chunk.start;
            cursorTimestamp = Math.max(cursorTimestamp, chunk.start + chunk.length);
            appended = true;
        }

        return appended;
    }

    private Set<Long> directoriesToPoll() {
        LinkedHashSet<Long> directories = new LinkedHashSet<>();
        directories.add((long) Math.floor(cursorTimestamp / DIRECTORY_SECONDS));
        directories.add(java.lang.System.currentTimeMillis() / 1000L / DIRECTORY_SECONDS);
        return directories;
    }

    private boolean shouldAppend(Chunk chunk) {
        if (downloadedChunks.contains(chunk.key())) {
            return false;
        }

        if (lastChunkStart >= 0D) {
            return chunk.start > lastChunkStart;
        }

        return chunk.start + chunk.length > media.getStartTimestamp();
    }

    private List<Chunk> getChunks(long directory) throws Exception {
        String url = trimTrailingSlash(media.getAudioBaseUrl())
                + "/server/get_chunks/"
                + media.getStation()
                + "/"
                + directory
                + "/";

        JsonObject root = getJson(url);
        JsonObject data = root.getAsJsonObject(String.valueOf(directory));
        List<Chunk> chunks = new ArrayList<>();

        if (data == null || !data.has("chunks")) {
            return chunks;
        }

        String defaultBaseUrl = data.has("baseURL") ? data.get("baseURL").getAsString() : "";
        JsonArray array = data.getAsJsonArray("chunks");

        for (JsonElement element : array) {
            JsonObject value = element.getAsJsonObject();
            if (!value.has("filename") || !value.has("start") || !value.has("length")) {
                continue;
            }

            String baseUrl = value.has("base_url")
                    ? value.get("base_url").getAsString()
                    : defaultBaseUrl;

            chunks.add(new Chunk(
                    forceHttps(baseUrl),
                    value.get("filename").getAsString(),
                    value.get("start").getAsDouble(),
                    value.get("length").getAsDouble()
            ));
        }

        return chunks;
    }

    private void appendChunk(Chunk chunk, FileOutputStream output) throws Exception {
        String url = trimTrailingSlash(chunk.baseUrl) + "/" + chunk.filename;
        HttpURLConnection connection = openConnection(url);

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("RadioCut chunk returned status " + status + " for " + url);
            }

            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream chunkData = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[64 * 1024];
                int read;

                while ((read = input.read(buffer)) != -1) {
                    if (!running.get()) {
                        throw new InterruptedException("RadioCut recording stopped");
                    }
                    chunkData.write(buffer, 0, read);
                }

                output.write(chunkData.toByteArray());
            }
        } finally {
            connection.disconnect();
        }
    }

    private JsonObject getJson(String location) throws Exception {
        HttpURLConnection connection = openConnection(location);

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("RadioCut index returned status " + status + " for " + location);
            }

            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return new JsonParser().parse(reader).getAsJsonObject();
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String location) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(location).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Referer", media.getUrl());
        return connection;
    }

    private void sleepUntilNextPoll() throws InterruptedException {
        Thread.sleep(POLL_INTERVAL_MS);
    }

    private String forceHttps(String value) {
        return value != null && value.startsWith("http://")
                ? "https://" + value.substring("http://".length())
                : value;
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static class Chunk {
        private final String baseUrl;
        private final String filename;
        private final double start;
        private final double length;

        private Chunk(String baseUrl, String filename, double start, double length) {
            this.baseUrl = baseUrl;
            this.filename = filename;
            this.start = start;
            this.length = length;
        }

        private String key() {
            return filename + "@" + start;
        }
    }
}
