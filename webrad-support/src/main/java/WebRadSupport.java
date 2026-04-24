import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebRadSupport extends FFMPEGSupport {

    private static final Pattern WEBRAD_API_ENDPOINT_PATTERN = Pattern.compile("\\\"(?<api>https:\\/\\/api\\.webrad.io\\/data\\/streams.[^\\\"]*)");
    private static final Pattern WEBRAD_STREAMS_PATTERN = Pattern.compile("\\\"streams\\\":(?<streams>\\[.*[^\\]]\\])");

    public WebRadSupport(Context context) {
        super(context);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String[] split = location.split(location.contains("#") ? "#" : "/");
        String channel = split[split.length - 1];

        String content = getContent(location);

        Matcher matcher = WEBRAD_API_ENDPOINT_PATTERN.matcher(content);
        if (matcher.find()) {
            String apiEndpoint = matcher.group("api");
            split = apiEndpoint.split("/");
            split[split.length - 1] = channel;
            apiEndpoint = Arrays.asList(split).stream().collect(Collectors.joining("/"));

            content = getContent(apiEndpoint);

            if (content.contains("\"success\":true")) {
                matcher = WEBRAD_STREAMS_PATTERN.matcher(content);
                if (matcher.find()) {
                    String json = matcher.group("streams");

                    return json;
                }
            }
        }

        return "";
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*radioarg.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new WebRadAudioResolver(getWrappedContext()));
    }
}
