import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RudovideoSupport extends FFMPEGSupport {

    // canalnet.tv embeds the player in multiple ways:
    // - <iframe src='https://rudo.video/live/<channel>' ...>
    // - <iframe data-url=\"//rudo.video/live/<channel>/volume/0/\" src=\"\" ...>
    // Be permissive with attribute names, spacing and scheme.
    private static final Pattern RUDO_LINK_PATTERN = Pattern.compile(
            "(?i)(?:src|data-url|data-video-url)\\s*=\\s*(\\\"|')(?<link>(?:https?:)?//rudo\\.video/live/[^\\\"']+)(\\\"|')"
    );
    private static final Pattern RUDO_BASE_PATTERN = Pattern.compile("(?i)(?:https?:)?//rudo\\.video/live/(?<id>[^/\\\"']+)");

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

            if (rudoLink.startsWith("//")) {
                rudoLink = "https:" + rudoLink;
            }

            // Normalize to the base /live/<id> page even if an embed adds extra path segments.
            Matcher baseMatcher = RUDO_BASE_PATTERN.matcher(rudoLink);
            if (baseMatcher.find()) {
                String id = baseMatcher.group("id");
                rudoLink = "https://rudo.video/live/" + id;
            }

            return getContent(rudoLink);
        }

        return "";
    }
}
