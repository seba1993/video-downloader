import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Arrays;
import java.util.HashMap;
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

    private String getPolicyKey(String content) {
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
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent(location);

        final String account = getAccount(content);
        final String video = getVideo(content);
        final String player = getPlayer(content);
        final String policyKey = getPolicyKey(content);

        if (account == null || video == null || player == null || policyKey == null) return "";

        HashMap<String, String> headers = new HashMap<>();
        headers.put("BCOV-Policy", policyKey);

        String playbackEndpoint = String.format(BRIGHTCOVE_PLAYBACK_ENDPOINT, account, video);

        content = getContent(playbackEndpoint, headers);

        return content;
    }
}
