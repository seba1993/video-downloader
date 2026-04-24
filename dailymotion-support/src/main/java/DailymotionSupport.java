import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DailymotionSupport extends FFMPEGSupport {

    private static final Pattern DAILYMOTION_VIDEO_ID_PATTERN = Pattern.compile("\\\"dm_player_live_dailymotion\\\":.*\\\"video_id\\\":\\\"(?<id>.[^\"]+)\\\"");

    private static final String DAILYMOTION_M3U8_ENDPOINT = "https://www.dailymotion.com/player/metadata/video/%s";

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
        return Map.of();
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
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
