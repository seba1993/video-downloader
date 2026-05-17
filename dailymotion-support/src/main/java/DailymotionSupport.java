import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaNotFoundException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.github.luischavez.videodownloader.support.Quality;
import com.github.luischavez.videodownloader.support.Video;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DailymotionSupport extends FFMPEGSupport {

    private static final Pattern DAILYMOTION_VIDEO_ID_PATTERN = Pattern.compile("\\\"dm_player_live_dailymotion\\\":.*\\\"video_id\\\":\\\"(?<id>.[^\"]+)\\\"");

    private static final String DAILYMOTION_M3U8_ENDPOINT = "https://www.dailymotion.com/player/metadata/video/%s";
    private static final String CNEWS_DAILYMOTION_URL = "https://www.dailymotion.com/video/x3b68jn";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";

    public DailymotionSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                Pattern.compile("^https?://.*cnews\\.fr.*$"),
                Pattern.compile("^https?://.*dailymotion\\.com/video/(?<video>.+)$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected Map<String, String> getHeaders(String location) {
        if (isCnewsLocation(location)) {
            return Map.of("User-Agent", USER_AGENT);
        }

        return Map.of();
    }

    private boolean isCnewsLocation(String location) {
        return location != null && (location.contains("cnews.fr") || location.contains("dailymotion.com/video/x3b68jn"));
    }

    private List<Media> resolveMediaWithYtDlp(String location) throws MediaOfflineException, MediaNotFoundException {
        final String workingDirectory = getWrappedContext().getWorkingDir();
        final File youtubeDirectory = new File(buildPath(workingDirectory, "youtube"));
        final File executableFile = new File(youtubeDirectory, "yt-dlp.exe");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    executableFile.getPath(),
                    "--ignore-config",
                    "--dump-single-json",
                    "--no-warnings",
                    "--skip-download",
                    location
            );
            processBuilder.directory(youtubeDirectory);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            if (exitCode != 0 || output.length() == 0) {
                throw new MediaOfflineException(String.format("yt-dlp failed for location %s", location));
            }

            JsonObject json = JsonParser.parseString(output.toString()).getAsJsonObject();
            JsonArray formats = json.getAsJsonArray("formats");

            if (formats == null || formats.size() == 0) {
                throw new MediaNotFoundException(String.format("formats not found for location %s", location));
            }

            List<Video> medias = new ArrayList<>();

            for (JsonElement element : formats) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject format = element.getAsJsonObject();

                if (!format.has("url") || !format.has("height") || format.get("height").isJsonNull()) {
                    continue;
                }

                int height = format.get("height").getAsInt();
                int width = format.has("width") && !format.get("width").isJsonNull() ? format.get("width").getAsInt() : 0;
                String url = format.get("url").getAsString();
                String info = format.has("format") && !format.get("format").isJsonNull() ? format.get("format").getAsString() : "CNEWS";
                int bandwidth = format.has("tbr") && !format.get("tbr").isJsonNull() ? (int) (format.get("tbr").getAsDouble() * 1000) : 0;

                Quality.Type qualityType = height >= 720 ? Quality.Type.HIGH : height >= 360 ? Quality.Type.MEDIUM : Quality.Type.LOW;
                medias.add(new Video(info, url, new Video.VideoQuality(qualityType, width, height, bandwidth), "", true));
            }

            if (medias.isEmpty()) {
                throw new MediaNotFoundException(String.format("formats not found for location %s", location));
            }

            Video lowest = medias.stream()
                    .min(Comparator.comparingInt(media -> ((Video.VideoQuality) media.getQuality()).getHeight()))
                    .orElse(medias.get(0));

            return java.util.Collections.singletonList(lowest);
        } catch (MediaOfflineException | MediaNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaOfflineException(String.format("yt-dlp request failed for location %s", location), ex);
        }
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        if (isCnewsLocation(location)) {
            return new String[0];
        }

        String[] links = super.getLinks(location);

        return Arrays.asList(links).stream()
                .filter(link -> link.contains("sec="))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        String[] split = mediaLink.split("\\.m3u8");
        mediaLink = split[0] + ".m3u8";

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String generateCommand(String location, Media media, String outputFile) {
        String command = super.generateCommand(location, media, outputFile);

        if (isCnewsLocation(location)) {
            command = command.replace(
                    " -c:v copy -c:a copy ",
                    " -map 0:v:0 -map 0:a:0 -c:v copy -c:a aac -profile:a aac_low -b:a 128k "
            );
        }

        return command;
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        if (isCnewsLocation(location)) {
            return resolveMediaWithYtDlp(CNEWS_DAILYMOTION_URL);
        }

        return super.getMedia(location);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        Matcher dailymotionPageMatcher = getLocationPatterns()[1].matcher(location);

        String videoId = null;
        if (dailymotionPageMatcher.matches()) {
            videoId = dailymotionPageMatcher.group("video");
        } else {
            String content = getContent(location);

            Matcher matcher = DAILYMOTION_VIDEO_ID_PATTERN.matcher(content);
            if (matcher.find()) {
                videoId = matcher.group("id");
            }
        }

        if (videoId != null) {
            String m3u8Endpoint = String.format(DAILYMOTION_M3U8_ENDPOINT, videoId);

            return getContent(m3u8Endpoint);
        }

        return "";
    }
}
