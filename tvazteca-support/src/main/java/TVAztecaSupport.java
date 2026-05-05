import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.github.luischavez.videodownloader.util.CryptoUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVAztecaSupport extends FFMPEGSupport {

    private static final Pattern MTUO_VIDEO_ID = Pattern.compile("data-mtuo-id=\\\"(?<link>.[^\\\"]+)\\\"");

    public TVAztecaSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*tvazteca\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher matcher = MTUO_VIDEO_ID.matcher(content);
        while (matcher.find()) {
            String link = matcher.group("link");

            return CryptoUtils.base64Decode(link);
        }

        // Newer pages sometimes embed the HLS URL directly in the HTML (e.g. Uplynk).
        // Returning the page content lets the base extractor find the .m3u8.
        return content;
    }
}
