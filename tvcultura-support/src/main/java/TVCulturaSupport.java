import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TVCulturaSupport extends FFMPEGSupport {

    public TVCulturaSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*cultura\\.uol\\.com\\.br.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected Map<String, String> resolveLinkHeaders(String location, String link) {
        return Map.of(
                "Host", "evpp.mm.uol.com.br",
                "Origin", "https://cultura.uol.com.br",
                "Referer", location);
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
        return getContent(location);
    }
}
