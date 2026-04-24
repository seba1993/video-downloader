import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LiveNewsMagSupport extends FFMPEGSupport {

    public LiveNewsMagSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*livenewsmag\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (!mediaLink.contains("http")) {
            String[] split = parentLink.split("/");
            split[split.length - 1] = mediaLink;
            mediaLink = Arrays.asList(split).stream().collect(Collectors.joining("/"));
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        return getContent(location);
    }
}
