import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DasersteSupport extends FFMPEGSupport {

    // Updated: the old mcdn.daserste.de master no longer works.
    private static final String DASERSTE_M3U8_MAIN_LINK = "https://daserste-live.ard-mcdn.de/daserste/live/hls/de/master.m3u8";

    public DasersteSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*daserste\\.de.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        String[] split = parentLink.split("/");
        split[split.length - 1] = mediaLink;

        mediaLink = Arrays.asList(split).stream().collect(Collectors.joining("/"));

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        return DASERSTE_M3U8_MAIN_LINK;
    }
}
