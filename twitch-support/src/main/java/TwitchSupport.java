import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.FFMPEGSupport;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaOfflineException;
import com.github.luischavez.videodownloader.support.MediaResolver;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchSupport extends FFMPEGSupport {

    private static final Pattern TWITCH_URL_PATTERN = Pattern.compile("https?://.*twitch\\.tv/(?<channel>.*)");
    private static final Pattern TWITCH_LINK_PATTERN = Pattern.compile("(?<link>https://player\\.twitch\\.tv/?\\?channel=(?<channel>.[^\\\"&]+))");

    private static final String TWITCH_TOKEN_ENDPOINT = "http://api.twitch.tv/api/channels/%s/access_token?client_id=%s";
    private static final String TWITCH_M3U8_ENDPOINT = "http://usher.twitch.tv/api/channel/hls/%s.m3u8?player=twitchweb&token=%s&sig=%s&$allow_audio_only=true&allow_spectre=false&allow_source=true&type=any&p=%d";

    private static final String TWITCH_PRIVATE_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko";

    private static final String TWITCH_GQL = "https://gql.twitch.tv/gql";

    public TwitchSupport(Context context) {
        super(context);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                TWITCH_URL_PATTERN,
                Pattern.compile("^https?://.*2ndrun\\.tv.*$"),
                Pattern.compile("^https?://.*tv-arg\\.net.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    private TwitchAuth auth(String channel) throws MediaOfflineException {
        try {
            String authEndpoint = TWITCH_GQL;

            HttpPost post = new HttpPost(authEndpoint);
            post.addHeader("Client-ID", TWITCH_PRIVATE_CLIENT_ID);
            post.setEntity(new StringEntity(String.format("{\"operationName\": \"PlaybackAccessToken\", \"extensions\": {\"persistedQuery\": {\"version\": 1, \"sha256Hash\": \"0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712\"}}, \"variables\": {\"isLive\": true, \"login\": \"%s\", \"isVod\": false, \"vodID\": \"\", \"playerType\": \"embed\"}}", channel)));

            HttpResponse httpResponse = httpClient.execute(post);
            String authString = EntityUtils.toString(httpResponse.getEntity());

            TwitchAuth twitchAuth = new GsonBuilder().create().fromJson(authString, TwitchAuth.class);
            twitchAuth.data.stream.token = CryptoUtils.encodeUrl(twitchAuth.data.stream.token);

            return twitchAuth;
        } catch (Exception ex) {
            throw new MediaOfflineException("can't get twitch token for: " + channel, ex);
        }
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        Matcher matcher = TWITCH_URL_PATTERN.matcher(location);

        String link = location;
        String channel = null;

        if (matcher.find()) {
            channel = matcher.group("channel");
        } else {
            String content = getContent(location);

            matcher = TWITCH_LINK_PATTERN.matcher(content);

            if (matcher.find()) {
                link = matcher.group("link");
                channel = matcher.group("channel");
            }
        }

        if (channel != null) {
            TwitchAuth twitchAuth = auth(channel);

            if (twitchAuth == null) return "";

            int random = (int) (new Random(System.nanoTime()).nextDouble() * 999999);

            String m3u8Endpoint = String.format(TWITCH_M3U8_ENDPOINT, channel, twitchAuth.data.stream.token, twitchAuth.data.stream.sig, random);

            return m3u8Endpoint;
        }

        return "";
    }

    private static class TwitchAuth {

        @SerializedName("data")
        public Data data;

        @Override
        public String toString() {
            return "TwitchAuth{" +
                    "data=" + data +
                    '}';
        }

        private class Data {

            @SerializedName("streamPlaybackAccessToken")
            public StreamData stream;

            @Override
            public String toString() {
                return "Data{" +
                        "stream=" + stream +
                        '}';
            }
        }

        private class StreamData {

            @SerializedName("value")
            public String token;

            @SerializedName("signature")
            public String sig;

            @Override
            public String toString() {
                return "StreamData{" +
                        "token='" + token + '\'' +
                        ", sig='" + sig + '\'' +
                        '}';
            }
        }
    }
}
