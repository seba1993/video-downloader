import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.github.luischavez.videodownloader.util.PlatformUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class YouTubeSupport extends FFMPEGSupport {

    private static final long YT_DLP_TIMEOUT_SECONDS = 60L;

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("\\\"videoId\\\":\\\"(?<id>.[^\\\"]+)");
    private static final Pattern YOUTUBE_EMBED_PATTERN = Pattern.compile("https?://www\\.youtube\\.com/embed/(?<id>[A-Za-z0-9_-]{6,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_LIVE_PATTERN = Pattern.compile("https?://www\\.youtube\\.com/live/(?<id>[A-Za-z0-9_-]{6,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_DATA_VIDEO_PATTERN = Pattern.compile("id=\\\"(?<id>[A-Za-z0-9_-]{6,})\\\"[^>]+data-video=\\\"youtube\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITAG_PATTERN = Pattern.compile("/itag/(?<itag>\\d+)/");

    private static final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=%s";

    private static class YtDlpResult {
        private final int exitCode;
        private final String output;

        private YtDlpResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    public YouTubeSupport(Context context) {
        super(context);
    }

    private String resolveExecutable(String workingDirectory) {
        final String extension = PlatformUtils.isWindowsHost() ? ".exe" : "";
        final String ytDlpPath = buildPath(workingDirectory, "youtube", "yt-dlp" + extension);

        if (new java.io.File(ytDlpPath).exists()) return "yt-dlp" + extension;

        return "yt-dlp";
    }

    private YtDlpResult runYtDlp(File youtubeDirectory, File executableFile, boolean useBrowserCookies, String... arguments) throws Exception {
        ArrayList<String> command = new ArrayList<>();
        command.add(executableFile.getPath());
        command.add("--ignore-config");

        if (useBrowserCookies) {
            command.add("--cookies-from-browser");
            command.add("firefox");
            command.add("--js-runtimes");
            File bundledNode = new File(youtubeDirectory, PlatformUtils.isWindowsHost() ? "node.exe" : "node");
            command.add(bundledNode.isFile() ? "node:" + bundledNode.getAbsolutePath() : "node");
            command.add("--remote-components");
            command.add("ejs:github");
            command.add("--extractor-args");
            command.add("youtube:player_client=mweb");
        }

        command.addAll(Arrays.asList(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(youtubeDirectory);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuffer output = new StringBuffer();

        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            } catch (Exception ex) {
                output.append("ERROR: failed to read yt-dlp output: ")
                        .append(ex.getMessage())
                        .append('\n');
            }
        }, "YouTubeSupport-yt-dlp-output");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(YT_DLP_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            output.append("ERROR: yt-dlp timed out after ")
                    .append(YT_DLP_TIMEOUT_SECONDS)
                    .append(" seconds\n");
        }

        outputReader.join(5_000L);
        if (outputReader.isAlive()) {
            outputReader.interrupt();
        }

        return new YtDlpResult(finished ? process.exitValue() : -1, output.toString());
    }

    private String extractFirstVideoId(String output) {
        if (output == null) return null;

        for (String line : output.split("\\R")) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("WARNING:") || line.startsWith("ERROR:") || line.startsWith("[")) {
                continue;
            }

            if (line.matches("[A-Za-z0-9_-]{6,}")) {
                return line;
            }
        }

        return null;
    }

    private String resolveLocation(String location) {
        String lower = location.toLowerCase();

        if (lower.contains("streamfare.com/") || lower.contains("cnnbrasil.com.br/ao-vivo") || lower.contains("excelsior.com.mx/tv")) {
            try {
                String content = getContent(location);
                content = content.replace("\\/", "/");
                Matcher matcher = YOUTUBE_EMBED_PATTERN.matcher(content);

                if (matcher.find()) {
                    return String.format(YOUTUBE_LINK, matcher.group("id"));
                }

                matcher = YOUTUBE_LIVE_PATTERN.matcher(content);

                if (matcher.find()) {
                    return String.format(YOUTUBE_LINK, matcher.group("id"));
                }

                matcher = YOUTUBE_DATA_VIDEO_PATTERN.matcher(content);

                if (matcher.find()) {
                    return String.format(YOUTUBE_LINK, matcher.group("id"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (!lower.contains("youtube.com")) {
            return location;
        }

        if (lower.contains("watch?v=") || lower.endsWith("/live")) {
            return location;
        }

        if (!lower.contains("/streams")) {
            return location;
        }

        try {
            final String workingDirectory = getWorkingDir();
            final String executable = resolveExecutable(workingDirectory);
            final File youtubeDirectory = new File(buildPath(workingDirectory, "youtube"));
            final File executableFile = new File(youtubeDirectory, executable);

            String[] arguments = new String[]{
                    "--flat-playlist",
                    "--match-filter", "live_status = is_live",
                    "--print", "id",
                    "--playlist-end", "10",
                    location
            };

            YtDlpResult result = runYtDlp(youtubeDirectory, executableFile, true, arguments);
            String videoId = extractFirstVideoId(result.output);

            if (videoId != null) {
                return String.format(YOUTUBE_LINK, videoId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return location;
    }

    private String resolveFormat(Media media) {
        Matcher matcher = ITAG_PATTERN.matcher(media.getUrl());
        if (matcher.find()) return matcher.group("itag");

        if (media.getQuality() instanceof Video.VideoQuality) {
            return String.format("bestvideo[height<=%d]+bestaudio/best[height<=%d]",
                    Video.VideoQuality.class.cast(media.getQuality()).getHeight(),
                    Video.VideoQuality.class.cast(media.getQuality()).getHeight());
        }

        return "best";
    }

    private Quality.Type resolveQualityType(int height) {
        if (height >= 720) {
            return Quality.Type.HIGH;
        }

        if (height >= 360) {
            return Quality.Type.MEDIUM;
        }

        return Quality.Type.LOW;
    }

    private static String appendHeadersFragment(String url, JsonObject headers) {
        if (url == null || headers == null || headers.entrySet().isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("#__headers__")) {
            sb.append("#__headers__");
        }

        headers.entrySet().forEach(entry -> {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isJsonNull()) {
                return;
            }

            String value = entry.getValue().getAsString();
            if (value == null || value.trim().isEmpty()) {
                return;
            }

            sb.append("&")
                    .append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(value));
        });

        return sb.toString();
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception ex) {
            return value;
        }
    }

    private List<Media> resolveMediaWithYtDlp(String location) throws MediaOfflineException, MediaNotFoundException {
        final String workingDirectory = getWorkingDir();
        final String executable = resolveExecutable(workingDirectory);
        final File youtubeDirectory = new File(buildPath(workingDirectory, "youtube"));
        final File executableFile = new File(youtubeDirectory, executable);

        try {
            String[] arguments = new String[]{
                    "--dump-single-json",
                    "--no-warnings",
                    "--skip-download",
                    location
            };

            YtDlpResult result = runYtDlp(youtubeDirectory, executableFile, true, arguments);

            if (result.exitCode != 0 || result.output.length() == 0) {
                throw new MediaOfflineException(String.format("yt-dlp failed for location %s", location));
            }

            JsonObject json = JsonParser.parseString(result.output).getAsJsonObject();
            JsonArray formats = json.getAsJsonArray("formats");

            if (formats == null || formats.size() == 0) {
                throw new MediaNotFoundException(String.format("formats not found for location %s", location));
            }

            ArrayList<Media> medias = new ArrayList<>();
            JsonObject rootHeaders = json.has("http_headers") && json.get("http_headers").isJsonObject()
                    ? json.getAsJsonObject("http_headers")
                    : null;

            for (JsonElement element : formats) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject format = element.getAsJsonObject();

                if (!format.has("format_id") || !format.has("height") || !format.has("width")) {
                    continue;
                }

                int height = format.get("height").isJsonNull() ? 0 : format.get("height").getAsInt();
                int width = format.get("width").isJsonNull() ? 0 : format.get("width").getAsInt();

                if (height <= 0 || width <= 0) {
                    continue;
                }

                String protocol = format.has("protocol") && !format.get("protocol").isJsonNull()
                        ? format.get("protocol").getAsString()
                        : "";

                if (!protocol.toLowerCase().contains("m3u8")) {
                    continue;
                }

                String formatId = format.get("format_id").getAsString();
                String m3u8Url = format.has("url") && !format.get("url").isJsonNull()
                        ? format.get("url").getAsString()
                        : null;

                if (m3u8Url == null || m3u8Url.trim().isEmpty()) {
                    continue;
                }

                JsonObject formatHeaders = format.has("http_headers") && format.get("http_headers").isJsonObject()
                        ? format.getAsJsonObject("http_headers")
                        : null;

                JsonObject headers = formatHeaders != null ? formatHeaders : rootHeaders;
                m3u8Url = appendHeadersFragment(m3u8Url, headers);

                int bandwidth = format.has("tbr") && !format.get("tbr").isJsonNull()
                        ? (int) Math.round(format.get("tbr").getAsDouble() * 1000)
                        : 0;

                medias.add(new Video(
                        formatId,
                        m3u8Url,
                        new Video.VideoQuality(resolveQualityType(height), width, height, bandwidth),
                        "",
                        true
                ));
            }

            if (medias.isEmpty()) {
                throw new MediaNotFoundException(String.format("media not found for location %s", location));
            }

            return medias;
        } catch (MediaOfflineException | MediaNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaOfflineException(String.format("yt-dlp request failed for location %s", location), ex);
        }
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                Pattern.compile("^https?://.*youtube\\.com.*$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/abc-news-australia-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/africa-news-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/cbc-news-canada-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/euro-news-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/france-24-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/news-12-new-york-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/sky-news-live-stream\\/?$"),
                Pattern.compile("^https?://.*cnnbrasil\\.com\\.br\\/ao-vivo\\/?$"),
                Pattern.compile("^https?://.*excelsior\\.com\\.mx\\/tv\\/?$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String getAudioCopyCodec(Media media) {
        if (media instanceof Video && media.getQuality() instanceof Video.VideoQuality) {
            int height = Video.VideoQuality.class.cast(media.getQuality()).getHeight();

            // YouTube low HLS variants (144p/240p) currently expose HE-AAC audio.
            // Some players fail to play that audio reliably inside the recorded MKV,
            // so normalize only those low variants to AAC-LC during recording.
            if (height <= 240) {
                return "-c:a aac -profile:a aac_low -b:a 128k";
            }
        }

        return super.getAudioCopyCodec(media);
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        location = resolveLocation(location);
        String[] links = super.getLinks(location);

        return Arrays.asList(links).stream()
                .map(link -> {
                    link = CryptoUtils.decodeUrl(link);

                    if (link.endsWith("\\")) {
                        link = link.substring(0, link.length() - 1);
                    }

                    return link;
                })
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        return resolveMediaWithYtDlp(resolveLocation(location));
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        location = resolveLocation(location);
        String content = getContent(location);

        if (location.toUpperCase().contains("WATCH")) {
            return content;
        } else {
            Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(content);
            if (matcher.find()) {
                String videoId = matcher.group("id");
                String streamLink = String.format(YOUTUBE_LINK, videoId);

                content = getContent(streamLink);

                return content;
            }
        }

        return "";
    }

    @Override
    public Task generateTask(String location, Media media, Map<String, Object> params) {
        // Hybrid mode: yt-dlp is used only to resolve the real HLS url (done in getMedia),
        // and ffmpeg is used to record.
        location = resolveLocation(location);
        return super.generateTask(location, media, params);
    }
}
