import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ElNueveSupport extends FFMPEGSupport {

    private static final Pattern LIVE_PATTERN = Pattern.compile("LIVE_URL\\s*=\\s*'(?<live>.[^']+)'");
    private static final Pattern RSK_ENDPOINT_PATTERN = Pattern.compile("'(?<endpoint>https?.*rsk=)'");
    private static final Pattern SALT_PATTERN = Pattern.compile("Math\\.floor\\(Date\\.now\\(\\)\\s*\\/\\s*\\d+\\),'(?<salt>.[^']+)'");

    public ElNueveSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*elnueve\\.com\\.ar.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher liveMatcher = LIVE_PATTERN.matcher(content);
        Matcher rskEndpointMatcher = RSK_ENDPOINT_PATTERN.matcher(content);
        Matcher saltMatcher = SALT_PATTERN.matcher(content);

        if (liveMatcher.find() && rskEndpointMatcher.find() && saltMatcher.find()) {
            String live = liveMatcher.group("live");
            String rskEndpoint = rskEndpointMatcher.group("endpoint");
            String salt = saltMatcher.group("salt");

            if (!live.startsWith("http")) {
                live = "https:" + live;
            }

            String rsk = rsk(salt);

            String tokenResponse = getContent(rskEndpoint + rsk);

            try {
                Token token = new GsonBuilder().create().fromJson(tokenResponse, Token.class);

                if (token.isSuccess()) {
                    return new String[] {
                            live + "?iut=" + token.getToken()
                    };
                }
            } catch (Exception ex) {
                throw new MediaOfflineException("can't get token " + location, ex);
            }
        }

        return new String[0];
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (!mediaLink.startsWith("http")) {
            String[] split = parentLink.split("/");
            split[split.length - 1] = mediaLink;
            mediaLink = Arrays.asList(split).stream().collect(Collectors.joining("/"));
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        return "";
    }

    private String rsk(int seed, String salt) {
        String[] splittedSalt = salt.split("");
        int length = splittedSalt.length;

        for (int i = length - 1; i >= 0; i--) {
            int r = (i * seed) % length;
            String v = splittedSalt[i];
            splittedSalt[i] = splittedSalt[r];
            splittedSalt[r] = v;
        }

        String joined = Arrays.asList(splittedSalt).stream().collect(Collectors.joining());

        String ok = joined.substring(length - 2, length);

        if (ok.equals("OK")) return joined.substring(0, length - 2);

        return null;
    }

    private String rsk(String salt) {
        long now = System.currentTimeMillis();

        int seed = (int) Math.floor(now / 3600000);

        String rsk = rsk(seed, salt);
        String rsk2 = rsk(seed - 1, salt);

        return rsk == null ? rsk2 : rsk;
    }

    private static class Token {

        @SerializedName("success")
        private boolean success;

        @SerializedName("token")
        private String token;

        public boolean isSuccess() {
            return success;
        }

        public String getToken() {
            return token;
        }
    }
}
