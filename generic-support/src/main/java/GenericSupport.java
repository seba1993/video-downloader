import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenericSupport extends FFMPEGSupport {

    private static final Pattern LINK_PATTERN = Pattern.compile("(?<link>(https?):\\\\?\\/\\\\?\\/[-a-zA-Z0-9+&@#\\/%?=~_|!:,.\\*;\\[\\]\\\\]*[-a-zA-Z0-9+&@#\\/%=~_|\\[\\]\\\\\\*])");

    private static final Pattern EMBED_LINK_PATTERN = Pattern.compile("\"embedUrl\"\\s*:\\s*\"(?<link>.+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(?<json>.+)</script>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern JWPLAYER_FILE_PATTERN = Pattern.compile("file\\s*:\\s*[\"'](?<link>https?:[^\"']+\\.m3u8[^\"']*)[\"']", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern STREAM_URL_PATTERN = Pattern.compile("streamURL\\s*=\\s*[\"'](?<link>https?:[^\"']+\\.m3u8[^\"']*)[\"']", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern CLAPPR_SOURCE_PATTERN = Pattern.compile("source\\s*:\\s*[\"'](?<link>https?:[^\"']+\\.m3u8[^\"']*)[\"']", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern PLLRC_PATTERN = Pattern.compile("\"src\"\\s*:\\s*\"(?<src>[^\"]+)\"\\s*,\\s*\"quality\"\\s*:\\s*\"(?<quality>[^\"]*)\"\\s*,\\s*\"type\"\\s*:\\s*\"(?<type>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern STREAM_NAME_PATTERN = Pattern.compile("<div\\s+id=\"stream_name\"[^>]*name=\"(?<name>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern VIDGYOR_FUNC_PATTERN = Pattern.compile("\\.loadPlayer\\(.[^,]+,.[^,]+,(?<channel>.[^,]+),.[^,]+,.[^,]+\\)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final String JAVASCRIPT_VARIABLE = "(var|let)\\s+VARNAME\\s*=\\s*(\"|')(?<value>.+)(\"|');";

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36";

    public GenericSupport(Context context) {
        super(context);
    }

    private boolean isStreamfareYouTubeLocation(String location) {
        return location.contains("streamfare.com/abc-news-australia-live-stream")
                || location.contains("streamfare.com/africa-news-live-stream")
                || location.contains("streamfare.com/cbc-news-canada-live-stream")
                || location.contains("streamfare.com/euro-news-live-stream")
                || location.contains("streamfare.com/france-24-live-stream")
                || location.contains("streamfare.com/news-12-new-york-live-stream")
                || location.contains("streamfare.com/sky-news-live-stream");
    }

    @Override
    public boolean canHandle(String location) {
        if (isStreamfareYouTubeLocation(location)) {
            return false;
        }

        return super.canHandle(location);
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(
                // NEW
                Pattern.compile("^https?://.*biobiochile\\.cl.*$"),
                Pattern.compile("^https?://.*n-tv\\.de.*$"),
                Pattern.compile("^https?://.*cctv\\.com.*$"),
                Pattern.compile("^https?://.*cnbcindonesia\\.com.*$"),
                Pattern.compile("^https?://.*cnbctv18\\.com.*$"),
                Pattern.compile("^https?://.*indiatimes\\.com.*$"),
                Pattern.compile("^https?://.*zeebiz\\.com.*$"),
                Pattern.compile("^https?://.*skynews\\.com.*$"),
                Pattern.compile("^https?://.*cbc\\.ca.*$"),
                Pattern.compile("^https?://.*radio-canada\\.ca.*$"),
                Pattern.compile("^https?://.*cp24\\.com.*$"),
                Pattern.compile("^https?://.*ctvnews\\.ca.*$"),
                Pattern.compile("^https?://.*wionews\\.com.*$"),
                Pattern.compile("^https?://.*timesnownews\\.com.*$"),
                Pattern.compile("^https?://.*uol\\.com.*$"),
                Pattern.compile("^https?://.*cnnbrasil\\.com.*$"),
                Pattern.compile("^https?://.*globo\\.com.*$"),
                Pattern.compile("^https?://.*cnnchile\\.com.*$"),
                Pattern.compile("^https?://.*presstv\\.com.*$"),
                Pattern.compile("^https?://.*i24news\\.tv.*$"),
                Pattern.compile("^https?://.*13tv\\.co\\.il.*$"),
                Pattern.compile("^https?://.*kan\\.org\\.il\\/live\\/?$"),
                Pattern.compile("^https?://.*knesset\\.tv\\/live\\/?$"),
                Pattern.compile("^https?://.*mako\\.co\\.il.*$"),
                Pattern.compile("^https?://.*newslive\\.com\\/.*$"),
                Pattern.compile("^https?://.*tvpass\\.org\\/live\\/.*$"),
                Pattern.compile("^https?://.*thetvapp\\.to\\/tv\\/.*$"),
                Pattern.compile("^https?://.*usnewson\\.com\\/watch\\/.*$"),
                Pattern.compile("^https?://.*streamfare\\.info\\/oan-news\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.(info|com)\\/.*-live-stream\\/?$"),
                Pattern.compile("^https?://.*streamfare\\.com\\/news-12-new-york-live-stream\\/?$"),
                Pattern.compile("^https?://.*rte\\.ie.*$"),
                Pattern.compile("^https?://.*beritasatu\\.com.*$"),
                Pattern.compile("^https?://.*metrotvnews\\.com.*$"),
                Pattern.compile("^https?://.*okezone\\.com.*$"),
                Pattern.compile("^https?://.*inews\\.id.*$"),
                Pattern.compile("^https?://.*rainews\\.it.*$"),
                Pattern.compile("^https?://.*norbaonline\\.it.*$"),
                Pattern.compile("^https?://.*mediaset\\.it.*$"),
                Pattern.compile("^https?://.*or\\.jp.*$"),
                Pattern.compile("^https?://.*express\\.pk.*$"),
                Pattern.compile("^https?://.*ptv\\.com.*$"),
                Pattern.compile("^https?://.*cignalplay\\.com.*$"),
                Pattern.compile("^https?://.*cnnphilippines\\.com.*$"),
                Pattern.compile("^https?://.*gmanetwork\\.com.*$"),
                Pattern.compile("^https?://.*rtp\\.pt.*$"),
                Pattern.compile("^https?://.*sic\\.pt.*$"),
                Pattern.compile("^https?://.*iol\\.pt.*$"),
                Pattern.compile("^https?://.*ip\\.digital.*$"),
                Pattern.compile("^https?://.*livenewstime\\.com.*$"),
                // OLD
                Pattern.compile("^https?://.*cbsnews\\.com.*$"),
                Pattern.compile("^https?://.*planetnews\\.com.*$"),
                Pattern.compile("^https?://.*livenewsnow\\.com.*$"),
                Pattern.compile("^https?://.*telefe\\.com.*$"),
                Pattern.compile("^https?://.*adn40\\.mx.*$"),
                Pattern.compile("^https?://.*francetvinfo\\.fr.*$"),
                Pattern.compile("^https?://.*tv5monde\\.com.*$"),
                Pattern.compile("^https?://.*welt\\.de.*$"),
                Pattern.compile("^https?://.*\\.m3u8.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new GenericVideoResolver(getWrappedContext()));
    }

    @Override
    protected String getAudioCopyCodec(Media media) {
        return super.getAudioCopyCodec(media);
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (location.contains("rainews.it")) {
            mediaLink = "https://streamcdne1-8e7439fdb1694c8da3a0fd63e4dda518.msvdn.net/rainews1/hls/" + mediaLink;
        } else {
            if (!mediaLink.toUpperCase().contains("HTTP")) {
                if (parentLink.contains("?")) {
                    parentLink = parentLink.split("\\?")[0];
                }

                String[] split = parentLink.split("/");
                split[split.length - 1] = mediaLink;
                mediaLink = Arrays.asList(split).stream().collect(Collectors.joining("/"));
            }
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String generateCommand(String location, Media media, String outputFile) {
        if (location.contains("ip.digital")) {
            media = new Video(media.getInfo(), "https://d1nmqgphjn0y4.cloudfront.net/live/ip/live.isml/5ee6e167-1167-4a85-9d8d-e08a3f55cff3.m3u8", new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 1000), "", true);
        }

        return super.generateCommand(location, media, outputFile);
    }

    @Override
    protected Map<String, String> getHeaders(String location) {
        Map<String, String> newHeaders = new HashMap<>();
        newHeaders.putAll(super.getHeaders(location));

        if (!location.contains("livenewsnow.com")) {
            newHeaders.put("User-Agent", USER_AGENT);
        }

        if (location.contains("kan.org.il")) {
            newHeaders.put("Origin", "https://www.kan.org.il");
        }

        if (location.contains("13tv.co.il")) {
            newHeaders.put("Origin", "https://13tv.co.il");
        }

        if (location.contains("usnewson.com")) {
            newHeaders.put("Referer", location);
            newHeaders.put("Origin", "https://usnewson.com");
        }

        if (location.contains("thetvapp.to")) {
            newHeaders.put("Referer", location);
            newHeaders.put("Origin", "https://thetvapp.to");
        }

        if (location.contains("tvpass.org")) {
            newHeaders.put("Referer", location);
            newHeaders.put("Origin", "https://tvpass.org");
        }

        /*String newLocation = location.replaceFirst("https?:\\/\\/", "");

        int indexOfFirstSlash = newLocation.indexOf("/");

        if (indexOfFirstSlash != -1) {
            newLocation = newLocation.substring(0, indexOfFirstSlash);
        }

        if (location.startsWith("https")) {
            location = "https://" + newLocation;
        } else {
            location = "http://" + newLocation;
        }

        newHeaders.put("Origin", location);*/

        return newHeaders;
    }

    @Override
    protected Map<String, String> resolveLinkHeaders(String location, String link) {
        if (location.contains("planetnews") || location.contains("livenewsnow.com")) {
            return Map.of("Referer", location, "User-Agent", USER_AGENT);
        }

        if (location.contains("kan.org.il")) {
            return Map.of(
                    "Referer", location,
                    "Origin", "https://www.kan.org.il",
                    "User-Agent", USER_AGENT
            );
        }

        if (location.contains("13tv.co.il")) {
            return Map.of(
                    "Referer", location,
                    "Origin", "https://13tv.co.il",
                    "User-Agent", USER_AGENT
            );
        }

        if (location.contains("usnewson.com")) {
            return Map.of(
                    "Referer", location,
                    "Origin", "https://usnewson.com",
                    "User-Agent", USER_AGENT
            );
        }

        if (location.contains("thetvapp.to")) {
            return Map.of(
                    "Referer", location,
                    "Origin", "https://thetvapp.to",
                    "User-Agent", USER_AGENT
            );
        }

        if (location.contains("tvpass.org")) {
            return Map.of(
                    "Referer", location,
                    "Origin", "https://tvpass.org",
                    "User-Agent", USER_AGENT
            );
        }

        return Map.of("User-Agent", USER_AGENT);
    }

    private String loadVidgyor(String channelName, String content) {
        Matcher matcher;

        if (!channelName.startsWith("\"") && !channelName.startsWith("'")) {
            Pattern pattern = Pattern.compile(JAVASCRIPT_VARIABLE.replace("VARNAME", channelName), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

            matcher = pattern.matcher(content);

            if (matcher.find()) {
                channelName = matcher.group("value");
            }
        }

        String vidgyor = "https://s3-ap-southeast-1.amazonaws.com/vidgyor.com/live/midroll/run/" + channelName + ".json";

        content = getContent(vidgyor);

        JsonObject jsonObject = (JsonObject) JsonParser.parseString(content);
        JsonObject cdn = jsonObject.getAsJsonObject("cdn");

        String liveStreamUrl = cdn.get("live_stream_url").getAsString();
        boolean tokenAuthEnabled = cdn.get("token_auth_enabled").getAsBoolean();
        String tokenAuthApiUrl = cdn.get("token_auth_api_url").getAsString();
        String tokenKey = cdn.get("token_key").getAsString();

        if (!tokenAuthEnabled) {
            return liveStreamUrl;
        }

        content = getContent(tokenAuthApiUrl);

        jsonObject = (JsonObject) JsonParser.parseString(content);

        String token = jsonObject.get(tokenKey).getAsString();

        if (!token.startsWith("http")) {
            token = liveStreamUrl + token;
        }

        return token;
    }
    
    private String resolveDefault(String location) throws MediaOfflineException {
        String content = getContent(location);

        if (content.toUpperCase().contains("\"EMBEDURL\"")) {
            Matcher matcher = EMBED_LINK_PATTERN.matcher(content);

            if (matcher.find()) {
                String link = matcher.group("link");
                link = link.replaceAll("\\/", "");
                link = link.split("\"")[0];
                link = link.replaceAll("\\\\", "/");

                content = getContent(link);
            }
        }

        if (content.toUpperCase().contains("VIDGYOR")) {
            Matcher matcher = VIDGYOR_FUNC_PATTERN.matcher(content);

            if (matcher.find()) {
                String channelName = matcher.group("channel");

                return loadVidgyor(channelName, content);
            }
        }

        if (content.toUpperCase().contains("#EXTINF")) {
            return location;
        }

        return resolveGeneric(location);
    }

    private String resolveIndiatimes(String location) throws MediaOfflineException {
        return "https://etnowweblive-lh.akamaihd.net/i/ETN_1@348070/master.m3u8";
    }

    private String resolveWionews(String location) throws MediaOfflineException {
        return loadVidgyor("zee_wion_us", getContent("https://www.wionews.com/live-tv"));
    }

    private String resolveInews(String location) throws MediaOfflineException {
        return "https://vcdn2.rctiplus.id/live/eds/inews_fta/live_fta/inews_fta-avc1_2000000=1-mp4a_64000_eng=2.m3u8";
    }

    private String resolveIpDigital(String location) throws MediaOfflineException {
        return "https://d1nmqgphjn0y4.cloudfront.net/live/ip/live.isml/5ee6e167-1167-4a85-9d8d-e08a3f55cff3.m3u8";
    }

    private String resolvePressTv(String location) throws MediaOfflineException {
        return "https://live.presstv.com/liveprs/smil:liveprs/playlist.m3u8";
    }

    private String resolveI24News(String location) throws MediaOfflineException {
        String hardwareId = String.valueOf(System.currentTimeMillis());
        String authEndpoint = String.format("https://api.i24news.wiztivi.io/authenticate?userName=I24News&hardwareId=%s&hardwareIdType=browser", hardwareId);

        String authContent = getContent(authEndpoint, Map.of(
                "Origin", "https://video.i24news.tv",
                "Referer", "https://video.i24news.tv/",
                "Content-Type", "application/json",
                "User-Agent", USER_AGENT
        ));

        JsonObject auth = JsonParser.parseString(authContent).getAsJsonObject();

        if (!auth.has("accessToken")) {
            return "";
        }

        String accessToken = auth.get("accessToken").getAsString();
        String contentsEndpoint = "https://api.i24news.wiztivi.io/contents?provider=brightcove&type=DYNAMIC&key=channel&value=all";
        String contents = getContent(contentsEndpoint, Map.of(
                "Origin", "https://video.i24news.tv",
                "Referer", "https://video.i24news.tv/",
                "Content-Type", "application/json",
                "Authorization", accessToken,
                "User-Agent", USER_AGENT
        ));

        JsonElement items = JsonParser.parseString(contents);

        if (items.isJsonObject()) {
            items = items.getAsJsonObject().get("value");
        }

        if (items == null || !items.isJsonArray()) {
            return "";
        }

        String[] parts = location.split("/");
        String channelId = parts.length == 0 ? "en" : parts[parts.length - 1].toLowerCase();

        for (JsonElement item : items.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }

            JsonObject channel = item.getAsJsonObject();

            if (!channel.has("id") || !channelId.equalsIgnoreCase(channel.get("id").getAsString())) {
                continue;
            }

            JsonObject customFields = channel.getAsJsonObject("customFields");

            if (customFields == null || !customFields.has("m3u8")) {
                return "";
            }

            return customFields.get("m3u8").getAsString();
        }

        return "";
    }

    private String resolveRainNews(String location) throws MediaOfflineException {
        String content = getContent(location);

        final Pattern pattern = Pattern.compile("mediaInfo\\.m3u8\\s*=\\s+'(?<link>.+)'");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String link = matcher.group("link");
            return link;
        }

        return "";
    }

    private String resolveNHKJP(String location) throws MediaOfflineException {
        return "https://nhkworld.webcdn.stream.ne.jp/www11/nhkworld-tv/global/2003458/live.m3u8";
    }

    private String resolvePtv(String location) throws MediaOfflineException {
        return "https://live.ptv.com.pk/live/ptvworld/playlist.m3u8";
    }

    private String resolveIol(String location) throws MediaOfflineException {
        String wmsAuthSign = getContent("https://services.iol.pt/matrix?userId=demo" + (new Random(System.currentTimeMillis()).nextInt()));

        return "https://video-auth7.iol.pt/live_edge/tvi24_abr/playlist.m3u8?wmsAuthSign=" + wmsAuthSign;
    }

    private String resolveCCTV(String location) throws MediaOfflineException {
        // return "https://cctvakhwh5c-cntv.akamaized.net/live/cdrmcctv2_1/index.m3u8";
        return "https://cctvakhwh5c-cntv.akamaized.net/live/cdrmcctv2_1/index.m3u8?BR=td&region=beijing";
    }

    private String resolveCBC(String location) throws MediaOfflineException {
        return "https://cbclivedai5-i.akamaihd.net/hls/live/567235/event2/CBOT/master5.m3u8";
    }

    private String resolveMetrotvnews(String location) throws MediaOfflineException {
        return "http://edge.metrotvnews.com:1935/live-edge/smil:metro.smil/playlist.m3u8";
    }

    private String resolveCNBCIndonesia(String location) throws MediaOfflineException {
        return "https://live.cnbcindonesia.com/livecnbc/smil:cnbctv.smil/playlist.m3u8";
    }

    private String resolveTimesNowNews(String location) throws MediaOfflineException {
        return "https://timesnow-lh.akamaihd.net/i/TNHD_1@129288/master.m3u8";
    }

    public String resolveRtp(String location) throws MediaOfflineException {
        String content = getContent(location);

        Pattern pattern = Pattern.compile("hls\\s*:\\s*decodeURIComponent\\(\\[(?<components>.+)\\]");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String components =matcher.group("components");
            String[] componentArray = components.split(",");

            String link = Arrays.asList(componentArray).stream()
                    .map(s -> s.replace("\"", ""))
                    .collect(Collectors.joining());

            try {
                link = URLDecoder.decode(link, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return link;
        }

        // Fallback: RTP pages may include the HLS URL directly in the HTML.
        // Returning content allows the generic link extractor to pick up .m3u8 URLs.
        return content;
    }

    public String resolveBiochile(String location) throws MediaOfflineException {
        return "https://unlimited1-us.dps.live/bbtv/bbtv.smil/playlist.m3u8";
    }

    private String getJsonString(JsonObject jsonObject, String... keys) {
        JsonElement current = jsonObject;

        for (String key : keys) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }

            JsonObject object = current.getAsJsonObject();

            if (!object.has(key) || object.get(key).isJsonNull()) {
                return null;
            }

            current = object.get(key);
        }

        if (current == null || !current.isJsonPrimitive()) {
            return null;
        }

        return current.getAsString();
    }

    private String resolve13TV(String location) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = NEXT_DATA_PATTERN.matcher(content);

        if (!matcher.find()) {
            return "";
        }

        JsonObject nextData = JsonParser.parseString(matcher.group("json")).getAsJsonObject();

        String[] preferredKeys = new String[]{
                getJsonString(nextData, "props", "pageProps", "liveSources", "desktop", "live_with_subs"),
                getJsonString(nextData, "props", "pageProps", "liveSources", "desktop", "live_no_subs"),
                getJsonString(nextData, "props", "pageProps", "liveSources", "mobile", "live_with_subs"),
                getJsonString(nextData, "props", "pageProps", "liveSources", "mobile", "live_no_subs")
        };

        for (String link : preferredKeys) {
            if (link == null || link.trim().isEmpty()) {
                continue;
            }

            try {
                getContent(link, resolveLinkHeaders(location, link));
                return link;
            } catch (MediaOfflineException ex) {
                // Try the next published variant.
            }
        }

        return "";
    }

    private String resolveKan(String location) throws MediaOfflineException {
        return "https://kanlivep2event-i.akamaihd.net/hls/live/747610/747610/master.m3u8";
    }

    private String resolveKnesset(String location) throws MediaOfflineException {
        return "https://kneset.gostreaming.tv/p2-kneset/_definst_/myStream/playlist.m3u8";
    }

    private String resolveMako(String location) throws MediaOfflineException {
        return "https://12channel.bonus-tv.ru/cdn/12channel_blackout/playlist.m3u8";
    }

    private String resolveOanNews(String location) throws MediaOfflineException {
        return "https://a-cdn.klowdtv.com/live1/oan_720p/playlist.m3u8";
    }

    private String resolveStreamfareClappr(String location) throws MediaOfflineException {
        if (location.contains("streamfare.info/")) {
            location = location.replace("streamfare.info/", "streamfare.com/");
        }

        String content = getContent(location);
        Matcher matcher = CLAPPR_SOURCE_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("link");
        }

        return resolveGeneric(location);
    }

    private String resolveNewsLive(String location) throws MediaOfflineException {
        String content = getContent(location);
        String normalizedContent = content.replace("\\/", "/");
        Matcher matcher = JWPLAYER_FILE_PATTERN.matcher(normalizedContent);

        if (matcher.find()) {
            return matcher.group("link");
        }

        matcher = STREAM_URL_PATTERN.matcher(normalizedContent);

        if (matcher.find()) {
            return matcher.group("link");
        }

        return resolveGeneric(location);
    }

    private String resolveLiveNewsNow(String location) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = STREAM_URL_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("link");
        }

        matcher = JWPLAYER_FILE_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("link");
        }

        return resolveGeneric(location);
    }

    private String resolveUSNewsON(String location) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = PLLRC_PATTERN.matcher(content);

        while (matcher.find()) {
            String src = matcher.group("src");
            String type = matcher.group("type");

            if ("hls".equalsIgnoreCase(type)) {
                return src;
            }

            if ("onestream".equalsIgnoreCase(type)) {
                String streamResponse = getContent(src, Map.of("User-Agent", USER_AGENT, "Referer", location));
                Matcher streamMatcher = LINK_PATTERN.matcher(streamResponse);

                while (streamMatcher.find()) {
                    String link = streamMatcher.group("link");

                    if (link.toUpperCase().contains(".M3U8")) {
                        return link;
                    }
                }
            }
        }

        return resolveGeneric(location);
    }

    private String buildCookieHeader(HttpResponse response) {
        Header[] cookieHeaders = response.getHeaders("Set-Cookie");

        if (cookieHeaders == null || cookieHeaders.length == 0) {
            return "";
        }

        return Arrays.stream(cookieHeaders)
                .map(Header::getValue)
                .map(value -> value.split(";", 2)[0])
                .collect(Collectors.joining("; "));
    }

    private String buildCookieHeader(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }

        List<String> cookieHeaders = headers.entrySet().stream()
                .filter(entry -> "set-cookie".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());

        if (cookieHeaders.isEmpty()) {
            return "";
        }

        return cookieHeaders.stream()
                .map(value -> value.split(";", 2)[0])
                .collect(Collectors.joining("; "));
    }

    private String readConnectionContent(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
        }

        reader.close();

        return builder.toString();
    }

    private String resolveTheTVApp(String location) throws MediaOfflineException {
        try {
            HttpURLConnection pageConnection = (HttpURLConnection) new URL(location).openConnection();
            pageConnection.setRequestMethod("GET");
            pageConnection.setRequestProperty("Referer", location);
            pageConnection.setRequestProperty("Origin", "https://thetvapp.to");
            pageConnection.setRequestProperty("User-Agent", USER_AGENT);

            int pageStatus = pageConnection.getResponseCode();

            if (!String.valueOf(pageStatus).startsWith("2")) {
                return "";
            }

            String content = readConnectionContent(pageConnection);
            String cookies = buildCookieHeader(pageConnection.getHeaderFields());
            Matcher matcher = STREAM_NAME_PATTERN.matcher(content);

            if (!matcher.find()) {
                return "";
            }

            String streamName = URLEncoder.encode(matcher.group("name"), "UTF-8");
            String tokenEndpoint = "https://thetvapp.to/token/" + streamName;
            HttpURLConnection tokenConnection = (HttpURLConnection) new URL(tokenEndpoint).openConnection();
            tokenConnection.setRequestMethod("GET");
            tokenConnection.setRequestProperty("Referer", location);
            tokenConnection.setRequestProperty("Origin", "https://thetvapp.to");
            tokenConnection.setRequestProperty("User-Agent", USER_AGENT);
            tokenConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            if (!cookies.isEmpty()) {
                tokenConnection.setRequestProperty("Cookie", cookies);
            }

            int tokenStatus = tokenConnection.getResponseCode();

            if (!String.valueOf(tokenStatus).startsWith("2")) {
                return "";
            }

            String tokenContent = readConnectionContent(tokenConnection);
            JsonObject tokenJson = JsonParser.parseString(tokenContent).getAsJsonObject();

            if (!tokenJson.has("url")) {
                return "";
            }

            return tokenJson.get("url").getAsString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String resolveTVPass(String location) throws MediaOfflineException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(location).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", location);
            connection.setRequestProperty("Origin", "https://tvpass.org");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int status = connection.getResponseCode();

            if (status >= 300 && status < 400) {
                String redirect = connection.getHeaderField("Location");
                return redirect == null ? "" : redirect;
            }

            if (!String.valueOf(status).startsWith("2")) {
                return "";
            }

            return resolveGeneric(location);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String resolveGeneric(String location) throws MediaOfflineException {
        String content = getContent(location);

        Matcher matcher = LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String link = matcher.group("link");

            if (!link.toUpperCase().contains(".M3U8")) continue;

            return link;
        }

        return content;
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        if (location.contains("rainews.it") || location.contains("i24news.tv") || location.contains("kan.org.il") || location.contains("knesset.tv") || location.contains("mako.co.il") || location.contains("newslive.com") || location.contains("livenewsnow.com") || location.contains("tvpass.org/live/") || location.contains("thetvapp.to/tv/") || location.contains("usnewson.com/watch/") || location.contains("streamfare.info/oan-news") || (location.contains("streamfare.") && location.contains("-live-stream") && !location.contains("news-12-new-york-live-stream"))) {
            return new String[]{resolveContent(location)};
        }

        return super.getLinks(location);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        if (location.contains("cctv.com")) {
            return resolveCCTV(location);
        } else if (location.contains("indiatimes.com")) {
            return resolveIndiatimes(location);
        } if (location.contains("wionews.com")) {
            return resolveWionews(location);
        } if (location.contains("inews.id")) {
            return resolveInews(location);
        } if (location.contains("ip.digital")) {
            return resolveIpDigital(location);
        } if (location.contains("presstv.com")) {
            return resolvePressTv(location);
        } if (location.contains("i24news.tv")) {
            return resolveI24News(location);
        } if (location.contains("rainews.it")) {
            return resolveRainNews(location);
        } if (location.contains("nhk.or.jp")) {
            return resolveNHKJP(location);
        } if (location.contains("ptv.com.pk")) {
            return resolvePtv(location);
        } if (location.contains("tvi24.iol.pt")) {
            return resolveIol(location);
        } else if (location.contains("cbc.ca")) {
            return resolveCBC(location);
        } else if (location.contains("metrotvnews.com")) {
            return resolveMetrotvnews(location);
        } else if (location.contains("cnbcindonesia.com")) {
            return resolveCNBCIndonesia(location);
        } else if (location.contains("timesnownews.com")) {
            return resolveTimesNowNews(location);
        } else if (location.contains("rtp.pt")) {
            return resolveRtp(location);
        } else if (location.contains("biobiochile.cl")) {
            return resolveBiochile(location);
        } else if (location.contains("13tv.co.il")) {
            return resolve13TV(location);
        } else if (location.contains("kan.org.il")) {
            return resolveKan(location);
        } else if (location.contains("knesset.tv")) {
            return resolveKnesset(location);
        } else if (location.contains("mako.co.il")) {
            return resolveMako(location);
        } else if (location.contains("newslive.com")) {
            return resolveNewsLive(location);
        } else if (location.contains("livenewsnow.com")) {
            return resolveLiveNewsNow(location);
        } else if (location.contains("tvpass.org/live/")) {
            return resolveTVPass(location);
        } else if (location.contains("thetvapp.to/tv/")) {
            return resolveTheTVApp(location);
        } else if (location.contains("usnewson.com/watch/")) {
            return resolveUSNewsON(location);
        } else if (location.contains("streamfare.info/oan-news")) {
            return resolveOanNews(location);
        } else if (location.contains("streamfare.") && location.contains("-live-stream") && !location.contains("news-12-new-york-live-stream")) {
            return resolveStreamfareClappr(location);
        } else {
            return resolveDefault(location);
        }
    }
}
