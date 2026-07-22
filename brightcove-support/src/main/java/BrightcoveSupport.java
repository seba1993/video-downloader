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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BrightcoveSupport extends FFMPEGSupport {

    private static final Pattern BRIGHTCOVE_ACCOUNT_PATTERN = Pattern.compile("data-account=\\\"(?<account>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_ACCOUNT_ALT_PATTERN = Pattern.compile("accountid=\\\"(?<account>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_VIDEO_PATTERN = Pattern.compile("data-video-id=\\\"(?<id>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_VIDEO_ALT_PATTERN = Pattern.compile("videoid=\\\"(?<id>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_PLAYER_PATTERN = Pattern.compile("data-player=\\\"(?<player>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_PLAYER_ALT_PATTERN = Pattern.compile("playerid=\\\"(?<player>.[^\"]+)\\\"");
    private static final Pattern BRIGHTCOVE_PK_PATTERN = Pattern.compile("policyKey:\\\"(?<pk>.[^\"]+)\\\"");

    private static final String BRIGHTCOVE_PLAYBACK_ENDPOINT = "https://edge.api.brightcove.com/playback/v1/accounts/%s/videos/%s";
    private static final String BRIGHTCOVE_POLICY_KEY_ENDPOINT = "https://players.brightcove.net/%s/%s_default/index.html?videoId=%s";
    private static final String BRIGHTCOVE_PK = "BCpkADawqM1mYQgRZ1bxuC1RqjjVAz6C5FCwu-68h_fyxNd0Ib4DDhZVlqC94kInbBuHvqkHQku1mZ5cRoyB3ISThApOKNpQX3iRai4hfGNbXfMhEr_FvqmfDHw";
    private static final String BFMTV_LIVE_URL = "https://www.bfmtv.com/en-direct/";
    private static final String BFMTV_LIVE_M3U8 = "https://live-cdn-stream-euw1.bfmtv.bct.nextradiotv.com/master.m3u8";
    private static final String BFMTV_BUSINESS_LIVE_M3U8 = "https://live-cdn-stream-euw1.bfmb.bct.nextradiotv.com/master.m3u8";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";

    public BrightcoveSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*bfmtv\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected Map<String, String> getHeaders(String location) {
        if (isBfmtvLiveLocation(location)) {
            return Map.of("User-Agent", USER_AGENT);
        }

        return super.getHeaders(location);
    }

    private boolean isBfmtvLiveLocation(String location) {
        return location != null && location.contains("bfmtv.com") && location.contains("/en-direct");
    }

    private String getBfmtvLiveM3u8(String location) {
        if (!isBfmtvLiveLocation(location)) {
            return null;
        }

        if (location.contains("/economie/")) {
            return BFMTV_BUSINESS_LIVE_M3U8;
        }

        return BFMTV_LIVE_M3U8;
    }

    private List<Media> resolveBfmtvLiveMedia(String location) throws MediaOfflineException, MediaNotFoundException {
        String liveM3u8 = getBfmtvLiveM3u8(location);

        if (liveM3u8 == null) {
            return resolveMediaWithYtDlp(location);
        }

        String info = location.contains("/economie/") ? "BFM Business live" : "BFM TV live";
        Video.VideoQuality quality = new Video.VideoQuality(Quality.Type.LOW, 480, 270, 450000);

        return java.util.Collections.singletonList(new Video(info, liveM3u8, quality, "", true));
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

                if (!format.has("height") || format.get("height").isJsonNull()) {
                    continue;
                }

                final String mediaUrl;
                if (format.has("manifest_url") && !format.get("manifest_url").isJsonNull()) {
                    mediaUrl = format.get("manifest_url").getAsString();
                } else if (format.has("url") && !format.get("url").isJsonNull()) {
                    mediaUrl = format.get("url").getAsString();
                } else {
                    continue;
                }

                int height = format.get("height").getAsInt();
                int width = format.has("width") && !format.get("width").isJsonNull() ? format.get("width").getAsInt() : 0;
                String info = format.has("format") && !format.get("format").isJsonNull() ? format.get("format").getAsString() : "BFMTV";
                int bandwidth = format.has("tbr") && !format.get("tbr").isJsonNull() ? (int) (format.get("tbr").getAsDouble() * 1000) : 0;

                Quality.Type qualityType = height >= 720 ? Quality.Type.HIGH : height >= 360 ? Quality.Type.MEDIUM : Quality.Type.LOW;
                medias.add(new Video(info, mediaUrl, new Video.VideoQuality(qualityType, width, height, bandwidth), "", true));
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

    private String getAccount(String content) {
        Matcher matcher = BRIGHTCOVE_ACCOUNT_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("account");
        }

        matcher = BRIGHTCOVE_ACCOUNT_ALT_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("account");
        }

        return null;
    }

    private String getVideo(String content) {
        Matcher matcher = BRIGHTCOVE_VIDEO_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("id");
        }

        matcher = BRIGHTCOVE_VIDEO_ALT_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("id");
        }

        return null;
    }

    private String getPlayer(String content) {
        Matcher matcher = BRIGHTCOVE_PLAYER_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("player");
        }

        matcher = BRIGHTCOVE_PLAYER_ALT_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("player");
        }

        return null;
    }

    private String getPolicyKey(String account, String player, String video) {
        String policyKeyEndpoint = String.format(BRIGHTCOVE_POLICY_KEY_ENDPOINT, account, player, video);

        String content = getContent(policyKeyEndpoint);

        Matcher matcher = BRIGHTCOVE_PK_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("pk");
        }

        return BRIGHTCOVE_PK;
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (!mediaLink.contains("http")) {
            if (mediaLink.startsWith("../")) {
                mediaLink = mediaLink.replace("../", "");
            }

            String[] parts = parentLink.split("\\/");
            parts[parts.length - 1] = "";
            parts[parts.length - 2] = mediaLink;

            mediaLink = Arrays.asList(parts).stream()
                    .collect(Collectors.joining("/"));
        }

        if (mediaLink.endsWith("/")) {
            mediaLink = mediaLink.substring(0, mediaLink.length() - 1);
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String generateCommand(String location, Media media, String outputFile) {
        String command = super.generateCommand(location, media, outputFile);

        if (isBfmtvLiveLocation(location)) {
            command = command.replace(
                    " -xerror ",
                    " -err_detect ignore_err -fflags +discardcorrupt -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 2 "
            );
            command = command.replace(
                    " -c:v copy -c:a copy ",
                    " -map 0:p:3:v:0 -map 0:p:3:a:0 -c:v copy -c:a copy "
            );
        }

        return command;
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        if (isBfmtvLiveLocation(location)) {
            return getContent(location);
        }

        String content = getContent(location);

        final String account = getAccount(content);
        final String video = getVideo(content);
        final String player = getPlayer(content);

        // Some pages embed a plain HLS URL without Brightcove metadata.
        // Returning the page content allows the base FFMPEGSupport link extractor to pick it up.
        if (account == null || video == null || player == null) return content;

        final String policyKey = getPolicyKey(account, player, video);

        if (policyKey == null) return "";

        HashMap<String, String> headers = new HashMap<>();
        headers.put("BCOV-Policy", policyKey);

        String playbackEndpoint = String.format(BRIGHTCOVE_PLAYBACK_ENDPOINT, account, video);

        content = getContent(playbackEndpoint, headers);

        return content;
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        if (isBfmtvLiveLocation(location)) {
            return resolveBfmtvLiveMedia(location);
        }

        return super.getMedia(location);
    }
}
