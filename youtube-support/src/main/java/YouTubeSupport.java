import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.util.CryptoUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeSupport extends ApacheHttpClientSupport<Video> {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("data-video-ids=\\\"(?<id>.[^\\\"]+)\\\"");

    private static final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=%s";

    @Injected
    public YouTubeSupport(Context context) {
        super(context);
    }

    @Override
    public Pattern[] getPatterns() {
        return patterns(Pattern.compile("^https?://.*youtube.com.*$"));
    }

    @Override
    public String getContent(String location) throws MediaOfflineException {
        String content = super.getContent(location);
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            String videoId = matcher.group("id");
            String streamLink = String.format(YOUTUBE_LINK, videoId);

            content = super.getContent(streamLink);

            List<String> links = findLinks(content, DEFAULT_M3U8_LINK_PATTERN);

            if (links.isEmpty()) return "";

            String link = links.get(0);
            link = link.replaceAll("\\\\/", "/");
            link = CryptoUtils.decodeUrl(link);

            return super.getContent(link);
        }

        return "";
    }

    @Override
    public MediaResolver<Video> getMediaResolver() {
        return getSystem().getDependencyInjection().make(M3U8VideoResolver.class);
    }

    @Override
    public Task generateTask(Media media, Map<String, Object> params) {
        String baseFileName = params.get("base_file_name").toString();
        String destinationPath = params.get("destination_path").toString();

        return new FFMPEGVideoTask(getWrappedContext(), media, baseFileName, destinationPath);
    }
}
