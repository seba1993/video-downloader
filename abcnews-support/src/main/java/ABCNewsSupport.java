import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ABCNewsSupport extends FFMPEGSupport {

    private static final Pattern ABC_LIVE_ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"(?<id>.[^\\\"]+)\\\"");

    private static final String ABC_LIVE_M3U8_ENDPOINT = "https://abcnews.go.com/video/itemfeed?id=%s&secure=true";

    public ABCNewsSupport(Context context) {
        super(context);
    }

    @Override
    protected String getAudioCopyCodec(Media media) {
        return "-c:a aac";
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*abcnews\\.go\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        String[] links = super.getLinks(location);

        return Arrays.asList(links).stream()
                .filter(link -> !link.toUpperCase().contains("PREVIEW"))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = super.getContent(location);

        Matcher matcher = ABC_LIVE_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            String id = matcher.group("id");

            String m3u8Endpoint = String.format(ABC_LIVE_M3U8_ENDPOINT, id);

            content = getContent(m3u8Endpoint);

            return content;
        }

        return "";
    }
}
