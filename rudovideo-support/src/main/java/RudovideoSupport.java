import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RudovideoSupport extends FFMPEGSupport {

    private static final Pattern RUDO_LINK_PATTERN = Pattern.compile("src=(\\\"|')(?<link>https(.[^\\\"]*)rudo\\.video\\/live(.[^\\\"']*))(\\\"|')");

    public RudovideoSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*canalnet\\.tv.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher matcher = RUDO_LINK_PATTERN.matcher(content);
        if (matcher.find()) {
            String rudoLink = matcher.group("link");

            return getContent(rudoLink);
        }

        return "";
    }
}
