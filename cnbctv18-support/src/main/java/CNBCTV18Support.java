import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.regex.Pattern;

public class CNBCTV18Support extends FFMPEGSupport {

    public CNBCTV18Support(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*cnbctv18\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent("https://www.cnbctv18.com/services/live-tv?id=cnbc-tv18");

        try {
            HLSResponse response = new GsonBuilder().create().fromJson(content, HLSResponse.class);

            String hls = response.getHls();

            if (hls != null && !hls.isEmpty()) {
                return hls;
            }
        } catch (Exception ex) {
            throw new MediaOfflineException("can't get hls list " + location, ex);
        }

        return "";
    }

    private static class HLSResponse {

        @SerializedName("hls")
        private String hls;

        @SerializedName("hls_single")
        private String hlsSingle;

        public String getHls() {
            return hls;
        }

        public String getHlsSingle() {
            return hlsSingle;
        }
    }
}
