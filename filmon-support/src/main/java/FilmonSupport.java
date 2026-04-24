import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;

import java.util.Map;
import java.util.regex.Pattern;

public class FilmonSupport extends FFMPEGSupport {

    public FilmonSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                Pattern.compile("^https?://.*filmon\\.com/channel/.*$"),
                Pattern.compile("^https?://.*filmon\\.com/tv/.*$")
        );
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        String[] links = super.getLinks(location);

        for (String link : links) {
            if (link.toUpperCase().contains("HIGH")) {
                return new String[] {link};
            }
        }

        return links;
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        return getContent(location, Map.of("X-Requested-With", "XMLHttpRequest"));
    }
}
