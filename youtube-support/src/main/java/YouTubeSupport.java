import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.util.CryptoUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class YouTubeSupport extends FFMPEGSupport {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("data-video-ids=\\\"(?<id>.[^\\\"]+)\\\"");

    private static final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=%s";

    @Injected
    public YouTubeSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*youtube.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        String[] links = super.getLinks(location);

        return Arrays.asList(links).stream()
                .map(link -> {
                    link = link.replaceAll("\\\\/", "/");
                    link = CryptoUtils.decodeUrl(link);

                    return link;
                })
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            String videoId = matcher.group("id");
            String streamLink = String.format(YOUTUBE_LINK, videoId);

            content = getContent(streamLink);

            return content;
        }

        return "";
    }
}
