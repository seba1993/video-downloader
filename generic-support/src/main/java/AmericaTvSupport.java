import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.MediaOfflineException;

import java.util.regex.Pattern;

public class AmericaTvSupport extends GenericSupport {

    private static final Pattern[] LOCATION_PATTERNS = new Pattern[] {
            Pattern.compile("^https?://.*americatv\\.com\\.ar\\/vivo\\/?$")
    };

    public AmericaTvSupport(Context context) {
        super(context);
    }

    @Override
    public boolean canHandle(String location) {
        return location != null && location.contains("americatv.com.ar/vivo");
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return LOCATION_PATTERNS;
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        if (canHandle(location)) {
            return new String[] {
                    "https://dai.google.com/linear/hls/pa/event/OY2i_lL4SMyXE5Zaj4ULEg/stream/12973818-8a4c-40da-83db-7c213537d815:SCL2/master.m3u8"
            };
        }

        return super.getLinks(location);
    }
}
