import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.github.luischavez.videodownloader.util.CryptoUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RadiomitreSupport extends FFMPEGSupport {

    public RadiomitreSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                Pattern.compile("^https?://.*radiomitre\\.cienradios\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected Map<String, String> getHeaders(String location) {
        return Map.of("Referer", "https://vmf.edge-apps.net");
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (!mediaLink.toUpperCase().contains("HTTP")) {
            String[] split = parentLink.split("/");
            split[split.length - 1] = mediaLink;
            mediaLink = Arrays.asList(split).stream().collect(Collectors.joining("/"));
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected Map<String, String> resolveLinkHeaders(String location, String link) {
        return Map.of("Referer", "https://vmf.edge-apps.net");
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher matcher = Pattern.compile("src=\\\"(?<link>.[^\\\"]*)\\\"").matcher(content);

        String mitreLink = null;
        while (matcher.find()) {
            String link = matcher.group("link");

            if (!link.contains("MITREHD")) continue;

            mitreLink = link;
            break;
        }

        if (mitreLink != null) {
            content = getContent(mitreLink,
                    Map.of(
                            "Referer", "https://radiomitre.cienradios.com/",
                            "Host", "vmf.edge-apps.net"));

            matcher = Pattern.compile("atob\\(\\\"(?<base64>.*[^\\\"])\\\"\\)").matcher(content);

            if (matcher.find()) {
                String base64 = matcher.group("base64");

                return CryptoUtils.base64Decode(base64);
            }
        }

        return "";
    }
}
