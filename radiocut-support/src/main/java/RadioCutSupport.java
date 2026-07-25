import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.support.MediaNotFoundException;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.Support;
import com.github.luischavez.videodownloader.task.Task;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RadioCutSupport extends ContextWrapper implements Support {

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?radiocut\\.fm/radiostation/([^/?#]+)/listen/?(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AUDIO_SECONDS_PATTERN = Pattern.compile(
            "class=[\"']audio_seconds[\"'][^>]*>\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AUDIO_BASE_URL_PATTERN = Pattern.compile(
            "class=[\"']audio_base_url[\"'][^>]*>\\s*(https?://[^<\\s]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final String DEFAULT_AUDIO_BASE_URL = "https://chunkserver-do.radiocut.site";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/138 Safari/537.36";

    public RadioCutSupport(Context context) {
        super(context);
    }

    @Override
    public boolean canHandle(String location) {
        return location != null && LOCATION_PATTERN.matcher(location).matches();
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        Matcher locationMatcher = LOCATION_PATTERN.matcher(location);
        if (!locationMatcher.matches()) {
            throw new MediaNotFoundException("invalid RadioCut URL " + location);
        }

        String station = locationMatcher.group(1);
        long timestamp = java.lang.System.currentTimeMillis() / 1000L;
        String audioBaseUrl = DEFAULT_AUDIO_BASE_URL;

        try {
            String page = getText(location, location);
            timestamp = parseTimestamp(page, location);
            audioBaseUrl = parseAudioBaseUrl(page);
        } catch (MediaOfflineException ex) {
            debug(RadioCutSupport.class, "RadioCut page unavailable; using the chunk server for " + station);
        }

        long directory = Math.floorDiv(timestamp, 10_000L);

        JsonObject index = getJson(buildIndexUrl(audioBaseUrl, station, directory), location);
        JsonObject directoryData = index.getAsJsonObject(String.valueOf(directory));

        if (directoryData == null
                || !directoryData.has("chunks")
                || directoryData.getAsJsonArray("chunks").size() == 0) {
            throw new MediaOfflineException("RadioCut has no audio chunks for " + station);
        }

        double latestStart = latestChunkStart(directoryData);
        double startTimestamp = latestStart >= 0D ? latestStart : timestamp;
        long currentDirectory = java.lang.System.currentTimeMillis() / 1000L / 10_000L;

        if (currentDirectory != directory) {
            try {
                JsonObject currentIndex = getJson(
                        buildIndexUrl(audioBaseUrl, station, currentDirectory),
                        location
                );
                JsonObject currentData = currentIndex.getAsJsonObject(String.valueOf(currentDirectory));
                if (currentData != null) {
                    startTimestamp = Math.max(startTimestamp, latestChunkStart(currentData));
                }
            } catch (MediaOfflineException ex) {
                debug(RadioCutSupport.class, "RadioCut current index is not available yet for " + station);
            }
        }

        return Collections.singletonList(new RadioCutMedia(
                location,
                station,
                audioBaseUrl,
                startTimestamp
        ));
    }

    @Override
    public Task generateTask(String location, Media media, Map<String, Object> params) {
        if (!(media instanceof RadioCutMedia)) {
            throw new IllegalArgumentException("invalid RadioCut media");
        }

        String baseFileName = String.valueOf(params.get("base_file_name"));
        String destinationPath = String.valueOf(params.get("destination_path"));

        return new RadioCutTask(
                getWrappedContext(),
                (RadioCutMedia) media,
                destinationPath,
                baseFileName + ".mp3"
        );
    }

    private long parseTimestamp(String page, String location) throws MediaOfflineException {
        Matcher matcher = AUDIO_SECONDS_PATTERN.matcher(page);
        if (!matcher.find()) {
            throw new MediaOfflineException("RadioCut timestamp not found in " + location);
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new MediaOfflineException("invalid RadioCut timestamp in " + location, ex);
        }
    }

    private String parseAudioBaseUrl(String page) {
        Matcher matcher = AUDIO_BASE_URL_PATTERN.matcher(page);
        return matcher.find() ? matcher.group(1) : DEFAULT_AUDIO_BASE_URL;
    }

    private String buildIndexUrl(String audioBaseUrl, String station, long directory) {
        return trimTrailingSlash(audioBaseUrl)
                + "/server/get_chunks/"
                + station
                + "/"
                + directory
                + "/";
    }

    private double latestChunkStart(JsonObject directoryData) {
        double latest = -1D;

        for (JsonElement element : directoryData.getAsJsonArray("chunks")) {
            JsonObject chunk = element.getAsJsonObject();
            if (chunk.has("start")) {
                latest = Math.max(latest, chunk.get("start").getAsDouble());
            }
        }

        return latest;
    }

    private JsonObject getJson(String url, String referer) throws MediaOfflineException {
        String content = getText(url, referer);

        try {
            return new JsonParser().parse(content).getAsJsonObject();
        } catch (Exception ex) {
            throw new MediaOfflineException("invalid RadioCut response from " + url, ex);
        }
    }

    private String getText(String location, String referer) throws MediaOfflineException {
        HttpURLConnection connection = null;

        try {
            connection = openConnection(location, referer);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new MediaOfflineException("RadioCut request failed with status " + status + " for " + location);
            }

            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[8192];
                int read;

                while ((read = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, read);
                }

                return content.toString();
            }
        } catch (MediaOfflineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaOfflineException("RadioCut request failed for " + location, ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String location, String referer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(location).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Referer", referer);
        return connection;
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
