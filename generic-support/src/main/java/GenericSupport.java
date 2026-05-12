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
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
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
    private static final Pattern HTML_SOURCE_PATTERN = Pattern.compile("<source[^>]+src=[\"'](?<link>https?:[^\"']+\\.m3u8[^\"']*)[\"']", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern PLLRC_PATTERN = Pattern.compile("\"src\"\\s*:\\s*\"(?<src>[^\"]+)\"\\s*,\\s*\"quality\"\\s*:\\s*\"(?<quality>[^\"]*)\"\\s*,\\s*\"type\"\\s*:\\s*\"(?<type>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern STREAM_NAME_PATTERN = Pattern.compile("<div\\s+id=\"stream_name\"[^>]*name=\"(?<name>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern AXIS_ID_PATTERN = Pattern.compile("\"axisId\"\\s*:\\s*\"?(?<id>\\d+)\"?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern GLOBAL_FEED_URL_PATTERN = Pattern.compile("\"feedUrl\"\\s*:\\s*\"(?<url>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern GLOBAL_MEDIA_ID_PATTERN = Pattern.compile("\"mediaId\"\\s*:\\s*\"(?<id>[0-9a-f\\-]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_M3U8_URL_PATTERN = Pattern.compile("\"m3u8_url\"\\s*:\\s*\"(?<link>https?:[^\"\\\\]*\\.m3u8[^\"\\\\]*)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_NATIONAL_M3U8_URL_PATTERN = Pattern.compile("\"national_m3u8_url\"\\s*:\\s*\"(?<link>https?:[^\"\\\\]*\\.m3u8[^\"\\\\]*)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_LOCAL_M3U8_TEMPLATE_PATTERN = Pattern.compile("\"m3u8Url\"\\s*:\\s*\"(?<link>https?:[^\"\\\\]*\\.m3u8[^\"\\\\]*)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_FW_WEB_AFID_PATTERN = Pattern.compile("\"fwWebAFIDNtl\"\\s*:\\s*\"(?<value>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_FW_WEB_SFID_PATTERN = Pattern.compile("\"fwWebSFIDNtl\"\\s*:\\s*\"(?<value>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_FW_NETWORK_ID_PATTERN = Pattern.compile("\"fwNetworkID\"\\s*:\\s*\"(?<value>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern NBC_CALL_LETTERS_PATTERN = Pattern.compile("\"callLetters\"\\s*:\\s*\"(?<value>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern ABC_DMP_PLAYBACK_TOKEN_PATTERN = Pattern.compile("\"dmpPlaybackToken\"\\s*:\\s*\"(?<token>[^\"]+)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern VIDGYOR_FUNC_PATTERN = Pattern.compile("\\.loadPlayer\\(.[^,]+,.[^,]+,(?<channel>.[^,]+),.[^,]+,.[^,]+\\)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final String JAVASCRIPT_VARIABLE = "(var|let)\\s+VARNAME\\s*=\\s*(\"|')(?<value>.+)(\"|');";

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36";
    private static final String ABC_REGISTER_DEVICE_BEARER = "YWJjJmJyb3dzZXImMS4wLjA.B1avqvrcbTb6GRneixWJGLgLCyVkVzOulkgaeD75Bys";
    private static final String ABC_CLIENT_ID = "abc-a9045cb5";
    private static final String ABC_SDK_VERSION = "34.1";
    private static final String ABC_APPLICATION_VERSION = "9.16.0";
    private static final String ABC_DEFAULT_PLAYBACK_ID = "eyJjaGFubmVsSWQiOiI3OTQ0OTMxMi03OWRkLTQ3M2QtODczYy01MTVlYmY0YjVlNWYiLCJjb250ZW50VHlwZSI6ImxpbmVhciIsInNvdXJjZUlkIjoiZGlzbmV5LWVudGVydGFpbm1lbnQtc3RhdGljIn0=";
    private static final String NBC_NIELSEN_APP_ID = "PE075FB87-C9AE-41D5-8B17-95C0E9301C8E";
    private static final String NBC_GPP = "DBABLA~BVQVAAAAAgA.QA";
    private static final String NBC_US_PRIVACY = "1YYN";
    private static final String NBC_PLAYER_VERSION = "8.30.1";

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

    private boolean isYouTubeBackedLocation(String location) {
        return isStreamfareYouTubeLocation(location)
                || location.contains("cnnbrasil.com.br/ao-vivo")
                || location.contains("excelsior.com.mx/tv");
    }

    @Override
    public boolean canHandle(String location) {
        if (isYouTubeBackedLocation(location)) {
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
                Pattern.compile("^https?://.*tvbrasilplay\\.com\\.br\\/tvs\\/?$"),
                Pattern.compile("^https?://.*cnbctv18\\.com.*$"),
                Pattern.compile("^https?://.*indiatimes\\.com.*$"),
                Pattern.compile("^https?://.*zeebiz\\.com.*$"),
                Pattern.compile("^https?://.*skynews\\.com.*$"),
                Pattern.compile("^https?://.*cbc\\.ca.*$"),
                Pattern.compile("^https?://.*radio-canada\\.ca.*$"),
                Pattern.compile("^https?://.*cp24\\.com.*$"),
                Pattern.compile("^https?://.*ctvnews\\.ca.*$"),
                Pattern.compile("^https?://.*globalnews\\.ca\\/live\\/.*$"),
                Pattern.compile("^https?://.*nbcnews\\.com\\/watch(?:#.*)?$"),
                Pattern.compile("^https?://.*abc\\.com\\/watch-live\\/.*$"),
                Pattern.compile("^https?://.*wionews\\.com.*$"),
                Pattern.compile("^https?://.*timesnownews\\.com.*$"),
                Pattern.compile("^https?://.*uol\\.com.*$"),
                Pattern.compile("^https?://.*cnnbrasil\\.com.*$"),
                Pattern.compile("^https?://.*globo\\.com.*$"),
                Pattern.compile("^https?://.*cnnchile\\.com.*$"),
                Pattern.compile("^https?://.*presstv\\.com.*$"),
                Pattern.compile("^https?://.*jovempan\\.com\\.br\\/ao-vivo\\/?$"),
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
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    protected String generateCommand(String location, Media media, String outputFile) {
        if (location.contains("ip.digital")) {
            media = new Video(media.getInfo(), "https://d1nmqgphjn0y4.cloudfront.net/live/ip/live.isml/5ee6e167-1167-4a85-9d8d-e08a3f55cff3.m3u8", new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 1000), "", true);
        }

        String command = super.generateCommand(location, media, outputFile);

        if (location.contains("abc.com/watch-live/")) {
            command = command.replace(
                    "ffmpeg -nostdin -xerror",
                    "ffmpeg -nostdin -xerror -allowed_extensions ALL -allowed_segment_extensions ALL -extension_picky 0 -protocol_whitelist file,http,https,tcp,tls,crypto,data"
            );
        }

        if (location.contains("i24news.tv")) {
            command = command.replace(
                    " -i \"",
                    " -i \""
            ).replace(
                    " -c:v copy -c:a copy ",
                    " -map 0:p:0:v:0 -map 0:p:0:a:0 -c:v copy -c:a copy "
            );
        }

        return command;
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
                link = link.replace("\\/", "/");
                link = link.split("\"")[0];
                link = link.replace("\\\\", "/");

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

    private String resolveBellMedia9c9ManifestUrl(String location, String destinationCode) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = AXIS_ID_PATTERN.matcher(content);

        if (!matcher.find()) {
            return "";
        }

        String axisId = matcher.group("id");
        String apiBaseUrl = String.format(
                "https://capi.9c9media.com/destinations/%s/platforms/desktop/contents/%s/",
                destinationCode,
                axisId
        );

        JsonObject jsonObject = JsonParser.parseString(
                fetchUrl(apiBaseUrl + "?$include=[Media.Name,Season,ContentPackages.Duration,ContentPackages.Id]")
        ).getAsJsonObject();

        JsonElement contentPackages = jsonObject.get("ContentPackages");
        if (contentPackages == null || !contentPackages.isJsonArray() || contentPackages.getAsJsonArray().size() == 0) {
            return "";
        }

        JsonObject firstPackage = contentPackages.getAsJsonArray().get(0).getAsJsonObject();
        if (!firstPackage.has("Id") || firstPackage.get("Id").isJsonNull()) {
            return "";
        }

        String packageId = firstPackage.get("Id").getAsString();
        return apiBaseUrl + "contentpackages/" + packageId + "/manifest.m3u8";
    }

    private String resolveBellMedia9c9(String location, String destinationCode) throws MediaOfflineException {
        String manifestUrl = resolveBellMedia9c9ManifestUrl(location, destinationCode);

        if (manifestUrl == null || manifestUrl.trim().isEmpty()) {
            return "";
        }

        return fetchUrl(manifestUrl);
    }

    private String resolveGlobalNewsMasterUrl(String location) throws MediaOfflineException {
        String content = getContent(location);
        String feedUrl = null;

        Matcher matcher = GLOBAL_FEED_URL_PATTERN.matcher(content);
        if (matcher.find()) {
            feedUrl = matcher.group("url");
        }

        if (feedUrl == null || feedUrl.trim().isEmpty() || feedUrl.contains("{")) {
            matcher = GLOBAL_MEDIA_ID_PATTERN.matcher(content);

            if (!matcher.find()) {
                return "";
            }

            String mediaId = matcher.group("id");
            feedUrl = "/gnca-ajax-redesign/video-entry/%7B%22id%22%3A%22" + mediaId + "%22%7D/";
        }

        feedUrl = feedUrl.replace("\\/", "/");

        if (!feedUrl.startsWith("http")) {
            feedUrl = "https://globalnews.ca" + feedUrl;
        }

        JsonElement root = JsonParser.parseString(fetchUrl(feedUrl));
        JsonObject payload = null;

        if (root.isJsonArray() && root.getAsJsonArray().size() > 0) {
            JsonElement first = root.getAsJsonArray().get(0);
            if (first.isJsonObject()) {
                payload = first.getAsJsonObject();
            }
        } else if (root.isJsonObject()) {
            payload = root.getAsJsonObject();
        }

        if (payload == null || !payload.has("sources") || !payload.get("sources").isJsonArray()) {
            return "";
        }

        for (JsonElement sourceElement : payload.getAsJsonArray("sources")) {
            if (!sourceElement.isJsonObject()) {
                continue;
            }

            JsonObject source = sourceElement.getAsJsonObject();
            String type = getJsonString(source, "type");
            String file = getJsonString(source, "file");

            if (file == null || file.trim().isEmpty()) {
                continue;
            }

            if (type == null || type.equalsIgnoreCase("hls")) {
                return file.replace("\\/", "/");
            }
        }

        return "";
    }

    private String resolveGlobalNews(String location) throws MediaOfflineException {
        String masterUrl = resolveGlobalNewsMasterUrl(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return fetchUrl(masterUrl);
    }

    private String normalizeNBCWatchHash(String location) {
        int index = location.indexOf('#');

        if (index == -1 || index == location.length() - 1) {
            return "";
        }

        return location.substring(index).toLowerCase();
    }

    private String getNBCNewsPortablePlayerUrl(String hash) {
        switch (hash) {
            case "#new-york":
                return "https://nbcnewyork.com/portableplayer/?CID=1:2:5351877&videoID=&origin=nbcnewyork.com&fullWidth=y&autoplay=true";
            case "#los-angeles":
                return "https://nbclosangeles.com/portableplayer/?CID=1:9:3396742&videoID=&origin=nbclosangeles.com&fullWidth=y&autoplay=true";
            case "#chicago":
                return "https://nbcchicago.com/portableplayer/?CID=1:6:3010684&videoID=214364229946&origin=nbcchicago.com&fullWidth=y&autoplay=true";
            case "#dallas-fort-worth":
                return "https://nbcdfw.com/portableplayer/?CID=1:8:3523824&videoID=&origin=nbcdfw.com&fullWidth=y&autoplay=true";
            case "#philadelphia":
                return "https://nbcphiladelphia.com/portableplayer/?CID=1:12:3841075&videoID=&origin=nbcphiladelphia.com&fullWidth=y&autoplay=true";
            case "#washington":
                return "https://nbcwashington.com/portableplayer/?CID=1:14:3600727&videoID=&origin=nbcwashington.com&fullWidth=y&autoplay=true";
            case "#boston":
                return "https://nbcboston.com/portableplayer/?CID=1:5:3349031&videoID=&origin=nbcboston.com&fullWidth=y&autoplay=true";
            case "#bay-area":
                return "https://nbcbayarea.com/portableplayer/?CID=1:4:3519840&videoID=&origin=nbcbayarea.com&fullWidth=y&autoplay=true";
            case "#miami":
                return "https://nbcmiami.com/portableplayer/?CID=1:10:3294236&videoID=&origin=nbcmiami.com&fullWidth=y&autoplay=true";
            case "#san-diego":
                return "https://nbcsandiego.com/portableplayer/?CID=1:13:3497501&videoID=&origin=nbcsandiego.com&fullWidth=y&autoplay=true";
            case "#connecticut":
                return "https://nbcconnecticut.com/portableplayer/?CID=1:7:3274515&videoID=&origin=nbcconnecticut.com&fullWidth=y&autoplay=true";
            case "#telemundo-florida":
                return "https://telemundo51.com/portableplayer/?CID=1:24:2525424&videoID=&origin=telemundo51.com&fullWidth=y&autoplay=true";
            case "#telemundo-noreste":
                return "https://telemundo47.com/portableplayer/?CID=1:25:2469836&videoID=&origin=telemundo47.com&fullWidth=y&autoplay=true";
            case "#telemundo-texas":
                return "https://telemundodallas.com/portableplayer/?CID=1:17:2422881&videoID=&origin=telemundodallas.com&fullWidth=y&autoplay=true";
            case "#telemundo-california":
                return "https://telemundo52.com/portableplayer/?CID=1:21:2641429&videoID=&origin=telemundo52.com&fullWidth=y&autoplay=true";
            default:
                return "";
        }
    }

    private String getNBCNewsDirectM3U8(String hash) {
        switch (hash) {
            case "":
                return "https://nnaa-nbcnn-lzaj01.fast.nbcuni.com/live/master.m3u8";
            case "#sports-now":
                return "https://g001-live-us-cmaf-prd-ak.pcdn03.cssott.com/Content/CMAF_OL2-CBC-4s/Live/channel(nbcsportspeacock)/master.m3u8";
            case "#allday":
                return "https://live-oneapp-prd-news.akamaized.net/Content/CMAF_OL2-CBC-4s/Live/channel(todayallday)/master.m3u8";
            case "#dateline":
                return "https://live-oneapp-prd-news.akamaized.net/Content/CMAF_OL2-CBC-4s/Live/channel(dateline)/master.m3u8";
            case "#skynews":
                return "https://live-oneapp-prd-news.akamaized.net/Content/CMAF_OL2-CBC-4s/Live/channel(skynews)/master.m3u8";
            case "#telemundo-al-dia":
                return "https://live-oneapp-prd.akamaized.net/Content/CMAF_OL2-CBC-4s/Live/channel(telemundoaldia)/master.m3u8";
            case "#telemundo-deportes-ahora":
                return "https://g001-live-us-cmaf-prd-ak.pcdn03.cssott.com/Content/CMAF_OL2-CBC-4s/Live/channel(telemundodeportes)/master.m3u8";
            case "#telemundo-florida":
                return "https://d368vp0qqzvkid.cloudfront.net/11603/88889703/hls/master.m3u8"
                        + "?ads.caid=TelemundoNoticiasFL"
                        + "&ads.csid=tm_ots_alldevice_allos_web_wscv_livelinear_virtualchannel"
                        + "&ads.nw=169843"
                        + "&ads.prof=169843%3Anbcu_ots_web_linear"
                        + "&ads.resp=vmap1"
                        + "&ads.sfid=23475402"
                        + "&ads.xumo_channelId=88889703a"
                        + "&ads.xumo_contentId=3905"
                        + "&ads.xumo_providerId=3905"
                        + "&ads.xumo_streamId=88889703"
                        + "&ads.site_name=TLMD";
            default:
                return "";
        }
    }

    private String decodeHtmlContent(String content) {
        return content
                .replace("\\/", "/")
                .replace("&quot;", "\"")
                .replace("&#034;", "\"")
                .replace("&#039;", "'")
                .replace("&#038;", "&")
                .replace("&amp;", "&");
    }

    private String extractPatternValue(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group("value");
    }

    private String encodeValue(String value) throws MediaOfflineException {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new MediaOfflineException("failed to encode NBC local value", ex);
        }
    }

    private String getNBCUserIp(String portablePlayerUrl) throws MediaOfflineException {
        try {
            URL url = new URL(portablePlayerUrl);
            String endpoint = url.getProtocol() + "://" + url.getHost() + "/wp-json/nbc/v1/ip?_locale=user";
            String ip = fetchUrl(endpoint).replace("\"", "").trim();

            if (!ip.isEmpty()) {
                return ip;
            }

            ip = fetchUrl("https://api.ipify.org").trim();

            if (!ip.isEmpty()) {
                return ip;
            }

            throw new MediaOfflineException("empty NBC local IP response " + portablePlayerUrl);
        } catch (Exception ex) {
            throw new MediaOfflineException("failed to resolve NBC local IP " + portablePlayerUrl, ex);
        }
    }

    private String resolveNBCLocalStation(String portablePlayerUrl) throws MediaOfflineException {
        String content = decodeHtmlContent(fetchUrl(toNBCNewsPartnerPlayerUrl(portablePlayerUrl)));
        Matcher matcher = NBC_LOCAL_M3U8_TEMPLATE_PATTERN.matcher(content);

        if (!matcher.find()) {
            return "";
        }

        String template = matcher.group("link");
        String afid = extractPatternValue(content, NBC_FW_WEB_AFID_PATTERN);
        String sfid = extractPatternValue(content, NBC_FW_WEB_SFID_PATTERN);
        String networkId = extractPatternValue(content, NBC_FW_NETWORK_ID_PATTERN);
        String callLetters = extractPatternValue(content, NBC_CALL_LETTERS_PATTERN).toLowerCase();
        String vip = getNBCUserIp(portablePlayerUrl);

        if (afid.trim().isEmpty() || sfid.trim().isEmpty() || networkId.trim().isEmpty() || callLetters.trim().isEmpty() || vip.trim().isEmpty()) {
            return "";
        }

        return template
                .replace("[APP_BUNDLE]", "web")
                .replace("[ATTS]", "")
                .replace("[USER_AGENT]", encodeValue(USER_AGENT))
                .replace("[LMT]", "0")
                .replace("[NIELSEN_APP_ID]", NBC_NIELSEN_APP_ID)
                .replace("[PLAYER_HEIGHT]", "720")
                .replace("[PLAYER_WIDTH]", "1280")
                .replace("[SITE_PAGE]", encodeValue(portablePlayerUrl))
                .replace("[US_PRIVACY]", NBC_US_PRIVACY)
                .replace("[AFID]", afid)
                .replace("[APP_NAME]", "nbcnews")
                .replace("[APP_VERSION]", "1.0")
                .replace("[CSID]", "nbc_ots_alldevice_allos_web_" + callLetters + "_livelinear_virtualchannel")
                .replace("[GPP_STRING_XXXXX]", encodeValue(NBC_GPP))
                .replace("[GPP_SID]", "7")
                .replace("[NW]", networkId)
                .replace("[PLAYER_VERSION]", NBC_PLAYER_VERSION)
                .replace("[PROF]", encodeValue(networkId + ":nbcu_ots_web_linear"))
                .replace("[SFID]", sfid)
                .replace("[VCID]", "")
                .replace("[VIP]", vip)
                .replace("[IFA]", "")
                .replace("[IFA_TYPE]", "dpid");
    }

    private String buildNBCNewsLocalFastUrl(String baseUrl,
                                            String portablePlayerUrl,
                                            String afid,
                                            String sfid,
                                            String caid,
                                            String channelName,
                                            String callLetters,
                                            String xumoChannelId,
                                            String xumoContentId,
                                            String xumoContentName,
                                            String xumoProviderId,
                                            String xumoProviderName,
                                            String xumoStreamId) throws MediaOfflineException {
        String vip = getNBCUserIp(portablePlayerUrl);

        return baseUrl
                + "?ads._fw_app_bundle=web"
                + "&ads._fw_atts="
                + "&ads._fw_h_user_agent=" + encodeValue(USER_AGENT)
                + "&ads._fw_is_lat=0"
                + "&ads._fw_nielsen_app_id=" + NBC_NIELSEN_APP_ID
                + "&ads._fw_player_height=720"
                + "&ads._fw_player_width=1280"
                + "&ads._fw_site_page=" + encodeValue(portablePlayerUrl)
                + "&ads._fw_us_privacy=" + NBC_US_PRIVACY
                + "&ads.afid=" + afid
                + "&ads.appName=nbcnews"
                + "&ads.appVersion=1.0"
                + "&ads.caid=" + caid
                + "&ads.channelName=" + channelName
                + "&ads.csid=nbc_ots_alldevice_allos_web_" + callLetters.toLowerCase() + "_livelinear_virtualchannel"
                + "&ads.flag=%2Bsltp%2Bemcr%2Bslcb%2Bsbid-fbad%2Baeti%2Bslif-vicb%2Bexvt%2Bamcb%2Bplay-uapl%2Bdtrd"
                + "&ads.gpp=" + encodeValue(NBC_GPP)
                + "&ads.gpp_sid=7"
                + "&ads.nw=169843"
                + "&ads.playerVersion=" + NBC_PLAYER_VERSION
                + "&ads.prof=" + encodeValue("169843:nbcu_ots_web_linear")
                + "&ads.resp=vmap1"
                + "&ads.sfid=" + sfid
                + "&ads.vcid="
                + "&ads.vip=" + vip
                + "&ads.xumo_channelId=" + xumoChannelId
                + "&ads.xumo_contentId=" + xumoContentId
                + "&ads.xumo_contentName=" + xumoContentName
                + "&ads.xumo_ifa="
                + "&ads.xumo_ifaType=dpid"
                + "&ads.xumo_providerId=" + xumoProviderId
                + "&ads.xumo_providerName=" + xumoProviderName
                + "&ads.xumo_streamId=" + xumoStreamId;
    }

    private String buildNBCChicagoWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d368vp0qqzvkid.cloudfront.net/11603/88889704/hls/master.m3u8",
                "https://www.nbcchicago.com/portableplayer/?CID=1:6:3010684&videoID=214364229946&origin=nbcchicago.com&fullWidth=y&autoplay=true",
                "396654828",
                "23408260",
                "NBCNCHI",
                "nbcchicagonews",
                "wmaq",
                "88889704a",
                "3818",
                "NBCNCHI",
                "3818",
                "NBCNCHI",
                "88889704"
        );
    }

    private String buildNBCNewYorkWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889709/hls/master.m3u8",
                "https://www.nbcnewyork.com/portableplayer/?CID=1:2:5351877&videoID=&origin=nbcnewyork.com&fullWidth=y&autoplay=true",
                "396654844",
                "23408240",
                "NBCNNY",
                "nbcnewyorknews",
                "wnbc",
                "88889709a",
                "3816",
                "NBCNNY",
                "3816",
                "NBCNNY",
                "88889709"
        );
    }

    private String buildNBCLosAngelesWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889710/hls/master.m3u8",
                "https://www.nbclosangeles.com/portableplayer/?CID=1:9:3396742&videoID=&origin=nbclosangeles.com&fullWidth=y&autoplay=true",
                "396654852",
                "23408248",
                "NBCNLA",
                "nbclosangelesnews",
                "knbc",
                "88889710a",
                "3817",
                "NBCNLA",
                "3817",
                "NBCNLA",
                "88889710"
        );
    }

    private String buildNBCBayAreaWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889711/hls/master.m3u8",
                "https://www.nbcbayarea.com/portableplayer/?CID=1:4:3519840&videoID=&origin=nbcbayarea.com&fullWidth=y&autoplay=true",
                "396654860",
                "23408256",
                "NBCNBA",
                "nbcbayareanews",
                "kntv",
                "88889711a",
                "3822",
                "NBCNBA",
                "3822",
                "NBCNBA",
                "88889711"
        );
    }

    private String buildNBCBostonWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889713/hls/master.m3u8",
                "https://www.nbcboston.com/portableplayer/?CID=1:5:3349031&videoID=&origin=nbcboston.com&fullWidth=y&autoplay=true",
                "396654876",
                "23408264",
                "NBCNBOS",
                "nbcbostonnews",
                "wbts",
                "88889713a",
                "3820",
                "NBCNBOS",
                "3820",
                "NBCNBOS",
                "88889713"
        );
    }

    private String buildNBCConnecticutWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889707/hls/master.m3u8",
                "https://www.nbcconnecticut.com/portableplayer/?CID=1:7:3274515&videoID=&origin=nbcconnecticut.com&fullWidth=y&autoplay=true",
                "396654812",
                "23408244",
                "NBCNCT",
                "nbcconnecticutnews",
                "wvit",
                "88889707a",
                "3832",
                "NBCNCT",
                "3832",
                "NBCNCT",
                "88889707"
        );
    }

    private String buildNBCDallasFortWorthWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d2kowtvrzzi7ps.cloudfront.net/11602/88889706/hls/master.m3u8",
                "https://www.nbcdfw.com/portableplayer/?CID=1:8:3523824&videoID=&origin=nbcdfw.com&fullWidth=y&autoplay=true",
                "396654804",
                "23408236",
                "NBCNDAL",
                "nbcdallasfortworthnews",
                "kdfw",
                "88889706a",
                "3831",
                "NBCNDAL",
                "3831",
                "NBCNDAL",
                "88889706"
        );
    }

    private String resolveNBCLowestVariantUrl(String location, String masterUrl) throws MediaOfflineException {
        String content = fetchUrl(masterUrl);
        String[] lines = content.split("\\r?\\n");
        String selectedLink = "";
        int selectedHeight = Integer.MAX_VALUE;
        int selectedBandwidth = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length - 1; i++) {
            String info = lines[i];
            String link = lines[i + 1].trim();

            if (!info.startsWith("#EXT-X-STREAM-INF")) {
                continue;
            }

            if (link.isEmpty() || link.startsWith("#")) {
                continue;
            }

            int height = 0;
            int bandwidth = 0;

            Matcher resolutionMatcher = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)").matcher(info);
            if (resolutionMatcher.find()) {
                height = Integer.parseInt(resolutionMatcher.group(2));
            }

            Matcher bandwidthMatcher = Pattern.compile("BANDWIDTH=(\\d+)").matcher(info);
            if (bandwidthMatcher.find()) {
                bandwidth = Integer.parseInt(bandwidthMatcher.group(1));
            }

            if (selectedLink.isEmpty()
                    || height < selectedHeight
                    || (height == selectedHeight && bandwidth < selectedBandwidth)) {
                selectedHeight = height;
                selectedBandwidth = bandwidth;
                selectedLink = link;
            }
        }

        if (selectedLink.isEmpty()) {
            return masterUrl;
        }

        try {
            URL base = new URL(masterUrl);
            java.net.URI resolved = new java.net.URI(base.getProtocol(), base.getAuthority(), base.getPath(), null, null).resolve(selectedLink.trim());
            if (resolved != null) {
                return resolved.toString();
            }
        } catch (Exception ex) {
            // fallback below
        }

        String resolved = buildMediaLink(location, masterUrl, selectedLink);
        if (resolved == null || resolved.trim().isEmpty()) {
            return masterUrl;
        }

        return resolved;
    }

    private String resolveNBCChicagoLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCChicagoWatchUrl());
    }

    private String resolveNBCNewYorkLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCNewYorkWatchUrl());
    }

    private String resolveNBCLosAngelesLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCLosAngelesWatchUrl());
    }

    private String resolveNBCBayAreaLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCBayAreaWatchUrl());
    }

    private String resolveNBCBostonLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCBostonWatchUrl());
    }

    private String resolveNBCConnecticutLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCConnecticutWatchUrl());
    }

    private String resolveNBCDallasFortWorthLowestVariantUrl(String location) throws MediaOfflineException {
        return resolveNBCLowestVariantUrl(location, buildNBCDallasFortWorthWatchUrl());
    }

    private String resolveNBCPhiladelphiaLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCSanDiegoLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCTelemundoCaliforniaLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCTelemundoFloridaLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCTelemundoNoresteLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCTelemundoTexasLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String resolveNBCDatelineLowestVariantUrl(String location) throws MediaOfflineException {
        // Dateline's master playlist has been unstable with the generic variant resolver.
        // Keep this pinned to the lowest direct rendition so the behavior is deterministic.
        return "https://live-oneapp-prd-news.akamaized.net/Content/CMAF_OL2-CBC-4s/Live/channel(dateline)/01_program.m3u8";
    }

    private String resolveNBCWashingtonLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = resolveNBCNewsWatch(location);

        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            return "";
        }

        return resolveNBCLowestVariantUrl(location, masterUrl);
    }

    private String buildNBCMiamiWatchUrl() throws MediaOfflineException {
        return buildNBCNewsLocalFastUrl(
                "https://d368vp0qqzvkid.cloudfront.net/11603/88889702/hls/master.m3u8",
                "https://www.nbcmiami.com/portableplayer/?CID=1:10:3294236&videoID=&origin=nbcmiami.com&fullWidth=y&autoplay=true",
                "396655091",
                "23408328",
                "NBCNFL",
                "nbcsouthfloridanews",
                "wtvj",
                "88889702a",
                "3823",
                "NBCNFL",
                "3823",
                "NBCNFL",
                "88889702"
        );
    }

    private String resolveNBCMiamiLowestVariantUrl(String location) throws MediaOfflineException {
        String masterUrl = buildNBCMiamiWatchUrl();
        String content = fetchUrl(masterUrl);
        String[] lines = content.split("\\r?\\n");
        String selectedLink = "";
        int selectedHeight = Integer.MAX_VALUE;
        int selectedBandwidth = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length - 1; i++) {
            String info = lines[i];
            String link = lines[i + 1].trim();

            if (!info.startsWith("#EXT-X-STREAM-INF")) {
                continue;
            }

            if (link.isEmpty() || link.startsWith("#")) {
                continue;
            }

            int height = 0;
            int bandwidth = 0;

            Matcher resolutionMatcher = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)").matcher(info);
            if (resolutionMatcher.find()) {
                height = Integer.parseInt(resolutionMatcher.group(2));
            }

            Matcher bandwidthMatcher = Pattern.compile("BANDWIDTH=(\\d+)").matcher(info);
            if (bandwidthMatcher.find()) {
                bandwidth = Integer.parseInt(bandwidthMatcher.group(1));
            }

            if (selectedLink.isEmpty()
                    || height < selectedHeight
                    || (height == selectedHeight && bandwidth < selectedBandwidth)) {
                selectedHeight = height;
                selectedBandwidth = bandwidth;
                selectedLink = link;
            }
        }

        if (selectedLink.isEmpty()) {
            return masterUrl;
        }

        try {
            URL base = new URL(masterUrl);
            java.net.URI resolved = new java.net.URI(base.getProtocol(), base.getAuthority(), base.getPath(), null, null).resolve(selectedLink.trim());
            if (resolved != null) {
                return resolved.toString();
            }
        } catch (Exception ex) {
            // fallback below
        }

        String resolved = buildMediaLink(location, masterUrl, selectedLink);
        if (resolved == null || resolved.trim().isEmpty()) {
            return masterUrl;
        }

        return resolved;
    }

    private String toNBCNewsPartnerPlayerUrl(String portablePlayerUrl) throws MediaOfflineException {
        try {
            URL url = new URL(portablePlayerUrl);
            String query = url.getQuery() == null ? "" : url.getQuery();
            String turl = url.getProtocol() + "://" + url.getHost() + url.getPath();
            return url.getProtocol() + "://" + url.getHost() + "/templates/nbc_partner_player?" + query
                    + (query.isEmpty() ? "" : "&")
                    + "turl=" + URLEncoder.encode(turl, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new MediaOfflineException("invalid NBC portable player url " + portablePlayerUrl, ex);
        }
    }

    private String normalizeNBCM3U8Url(String link) {
        return link
                .replace("PLATFORM", "desktopweb")
                .replace("APP_NAME", "nbcnews")
                .replace("APP_VERSION", "1.0")
                .replace("APP_BUNDLE", "web")
                .replace("DEVICE_MAKE", "desktop")
                .replace("DEVICE_MODEL", "desktop")
                .replace("DEVICE_TYPE", "2-Personal_Computer")
                .replace("LMT", "0")
                .replace("US_PRIVACY", "1---")
                .replace("GPP_SID", "7")
                .replace("GPP_STRING_XXXXX", "DBABLA~BVQVAAAAAgA.QA")
                .replace("PLAYER_WIDTH", "1280")
                .replace("PLAYER_HEIGHT", "720")
                .replace("SITE_NAME", "NBC")
                .replace("SITE_PAGE", "https%3A%2F%2Fwww.nbcnews.com%2Fwatch")
                .replace("APP_STORE_URL", "")
                .replace("IFA_TYPE", "dpid")
                .replace("IFA", "")
                .replace("[APP_BUNDLE]", "web")
                .replace("[ATTS]", "")
                .replace("[USER_AGENT]", USER_AGENT)
                .replace("[LMT]", "0")
                .replace("[NIELSEN_APP_ID]", "PE075FB87-C9AE-41D5-8B17-95C0E9301C8E")
                .replace("[PLAYER_HEIGHT]", "720")
                .replace("[PLAYER_WIDTH]", "1280")
                .replace("[SITE_PAGE]", "https%3A%2F%2Fwww.nbcnews.com%2Fwatch")
                .replace("[US_PRIVACY]", "1---")
                .replace("[AFID]", "")
                .replace("[APP_NAME]", "nbcnews")
                .replace("[APP_VERSION]", "1.0")
                .replace("[CSID]", "nbc_us_desktopweb_nbcnews_ssai")
                .replace("[GPP_STRING_XXXXX]", "DBABLA~BVQVAAAAAgA.QA")
                .replace("[GPP_SID]", "7")
                .replace("[NW]", "")
                .replace("[PLAYER_VERSION]", "8.30.1")
                .replace("[PROF]", "")
                .replace("[SFID]", "")
                .replace("[VCID]", "")
                .replace("[VIP]", "")
                .replace("[IFA]", "")
                .replace("[IFA_TYPE]", "dpid");
    }

    private String resolveNBCPortablePlayer(String portablePlayerUrl) throws MediaOfflineException {
        String partnerPlayerUrl = toNBCNewsPartnerPlayerUrl(portablePlayerUrl);
        String content = fetchUrl(partnerPlayerUrl)
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");

        Matcher matcher = NBC_M3U8_URL_PATTERN.matcher(content);

        if (matcher.find()) {
            return normalizeNBCM3U8Url(matcher.group("link"));
        }

        matcher = NBC_NATIONAL_M3U8_URL_PATTERN.matcher(content);

        if (matcher.find()) {
            return normalizeNBCM3U8Url(matcher.group("link"));
        }

        return "";
    }

    private String resolveNBCNewsWatch(String location) throws MediaOfflineException {
        String hash = normalizeNBCWatchHash(location);
        if ("#new-york".equals(hash)) {
            return resolveNBCNewYorkLowestVariantUrl(location);
        }

        if ("#los-angeles".equals(hash)) {
            return resolveNBCLosAngelesLowestVariantUrl(location);
        }

        if ("#bay-area".equals(hash)) {
            return resolveNBCBayAreaLowestVariantUrl(location);
        }

        if ("#boston".equals(hash)) {
            return resolveNBCBostonLowestVariantUrl(location);
        }

        if ("#connecticut".equals(hash)) {
            return resolveNBCConnecticutLowestVariantUrl(location);
        }

        if ("#dallas-fort-worth".equals(hash)) {
            return resolveNBCDallasFortWorthLowestVariantUrl(location);
        }

        if ("#dateline".equals(hash)) {
            return resolveNBCDatelineLowestVariantUrl(location);
        }

        if ("#chicago".equals(hash)) {
            return resolveNBCChicagoLowestVariantUrl(location);
        }

        if ("#miami".equals(hash)) {
            return resolveNBCMiamiLowestVariantUrl(location);
        }

        String direct = getNBCNewsDirectM3U8(hash);

        if (direct != null && !direct.trim().isEmpty()) {
            return direct;
        }

        String portablePlayerUrl = getNBCNewsPortablePlayerUrl(hash);

        if (portablePlayerUrl != null && !portablePlayerUrl.trim().isEmpty()) {
            return resolveNBCPortablePlayer(portablePlayerUrl);
        }

        if (hash.isEmpty()) {
            return getNBCNewsDirectM3U8("");
        }

        return "";
    }

    private String fetchUrl(String url) throws MediaOfflineException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int status = connection.getResponseCode();
            if (!String.valueOf(status).startsWith("2")) {
                throw new MediaOfflineException("invalid request " + url + " status " + status);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (MediaOfflineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaOfflineException("request failed, may be the site is offline or you don't have internet connection " + url, ex);
        }
    }

    private String postJson(String url, String body, Map<String, String> headers) throws MediaOfflineException {
        try {
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            headers.forEach(connection::setRequestProperty);

            connection.getOutputStream().write(data);

            int status = connection.getResponseCode();
            if (!String.valueOf(status).startsWith("2")) {
                throw new MediaOfflineException("invalid request " + url + " status " + status);
            }

            return readConnectionContent(connection);
        } catch (MediaOfflineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaOfflineException("request failed, may be the site is offline or you don't have internet connection " + url, ex);
        }
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String getRequiredString(JsonObject object, String field) throws MediaOfflineException {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            throw new MediaOfflineException("missing ABC response field " + field);
        }

        return object.get(field).getAsString();
    }

    private String resolveABCWatchLive(String location) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = ABC_DMP_PLAYBACK_TOKEN_PATTERN.matcher(content);
        String playbackId = matcher.find() ? matcher.group("token") : ABC_DEFAULT_PLAYBACK_ID;

        String registerBody = "{\"query\":\"mutation registerDevice($input: RegisterDeviceInput!) { registerDevice(registerDevice: $input) { grant { grantType assertion } } }\","
                + "\"variables\":{\"input\":{\"deviceFamily\":\"browser\",\"applicationRuntime\":\"chrome\",\"deviceProfile\":\"windows\",\"deviceLanguage\":\"en-US\","
                + "\"attributes\":{\"osDeviceIds\":[],\"manufacturer\":\"microsoft\",\"model\":null,\"operatingSystem\":\"windows\",\"operatingSystemVersion\":\"10.0\","
                + "\"browserName\":\"chrome\",\"browserVersion\":\"148.0.0\",\"brand\":\"web\"},\"devicePlatformId\":\"browser\"}},\"operationName\":\"registerDevice\"}";

        Map<String, String> registerHeaders = new HashMap<>();
        registerHeaders.put("Authorization", "Bearer " + ABC_REGISTER_DEVICE_BEARER);
        registerHeaders.put("X-BAMSDK-Client-ID", ABC_CLIENT_ID);
        registerHeaders.put("X-BAMSDK-Version", ABC_SDK_VERSION);
        registerHeaders.put("X-BAMSDK-Platform", "javascript/windows/chrome");
        registerHeaders.put("X-BAMSDK-Platform-ID", "browser");
        registerHeaders.put("X-DSS-Edge-Accept", "vnd.dss.edge+json; version=2");
        registerHeaders.put("X-Application-Version", ABC_APPLICATION_VERSION);
        registerHeaders.put("Origin", "https://abc.com");
        registerHeaders.put("Referer", location);

        JsonObject registerJson = JsonParser.parseString(
                postJson("https://disney-entertainment.api.edge.bamgrid.com/graph/v1/device/graphql", registerBody, registerHeaders)
        ).getAsJsonObject();

        JsonObject sdk = registerJson.getAsJsonObject("extensions").getAsJsonObject("sdk");
        String accessToken = getRequiredString(sdk.getAsJsonObject("token"), "accessToken");

        String playbackSessionId = java.util.UUID.randomUUID().toString();
        String standardWebId = java.util.UUID.randomUUID().toString();
        String playbackBody = "{\"playback\":{\"attributes\":{\"resolution\":{\"max\":[\"1280x720\"]},\"protocol\":\"HTTPS\","
                + "\"assetInsertionStrategies\":{\"point\":\"SGAI\",\"range\":\"SGAI\"},\"playbackInitiationContext\":\"ONLINE\","
                + "\"frameRates\":[60],\"videoSegmentTypes\":[\"FMP4\"],\"maxSlideDuration\":\"15_MIN\",\"promosSupported\":true},"
                + "\"adTracking\":{\"limitAdTrackingEnabled\":\"NOT_SUPPORTED\",\"deviceAdId\":\"00000000-0000-0000-0000-000000000000\","
                + "\"privacyOptOut\":\"YES\",\"additionalConsent\":\"\",\"gamParameters\":{\"adUnitCode\":\"/21783347309/abc-news/abc.com/web/fast-channel\","
                + "\"contextUrl\":\"" + jsonEscape(location) + "\",\"affiliateStationCode\":\"\",\"isPlayerMuted\":false,\"isPlayerAutoplay\":true}},"
                + "\"tracking\":{\"playbackSessionId\":\"" + playbackSessionId + "\",\"standardWebId\":\"" + jsonEscape(standardWebId) + "\"}},"
                + "\"playbackId\":\"" + jsonEscape(playbackId) + "\",\"allowedCreatives\":[],\"allowedInsertionVisuals\":[]}";

        Map<String, String> playbackHeaders = new HashMap<>();
        playbackHeaders.put("Authorization", "Bearer " + accessToken);
        playbackHeaders.put("X-BAMSDK-Client-ID", ABC_CLIENT_ID);
        playbackHeaders.put("X-BAMSDK-Version", ABC_SDK_VERSION);
        playbackHeaders.put("X-BAMSDK-Platform", "javascript/windows/chrome");
        playbackHeaders.put("X-BAMSDK-Platform-ID", "browser");
        playbackHeaders.put("X-DSS-Edge-Accept", "vnd.dss.edge+json; version=2");
        playbackHeaders.put("X-Application-Version", ABC_APPLICATION_VERSION);
        playbackHeaders.put("Origin", "https://abc.com");
        playbackHeaders.put("Referer", location);

        JsonObject playbackJson = JsonParser.parseString(
                postJson("https://disney-entertainment.playback.edge.bamgrid.com/v7/playback/tve/ctr-regular", playbackBody, playbackHeaders)
        ).getAsJsonObject();

        JsonObject firstSource = playbackJson.getAsJsonObject("stream")
                .getAsJsonArray("sources")
                .get(0)
                .getAsJsonObject();

        return getRequiredString(firstSource.getAsJsonObject("slide"), "url");
    }

    private String[] extractLinksFromText(String content) {
        java.util.ArrayList<String> links = new java.util.ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String link = matcher.group("link");

            if (link.contains("\\/")) {
                link = link.replace("\\/", "/");
            }

            if (!link.toUpperCase().contains(".M3U8")) {
                continue;
            }

            if (!links.contains(link)) {
                links.add(link);
            }
        }

        return links.toArray(new String[0]);
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

    private String resolveJovemPan(String location) throws MediaOfflineException {
        String content = getContent(location);
        Matcher matcher = HTML_SOURCE_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group("link").replace("&amp;", "&");
        }

        return resolveGeneric(location);
    }

    private String resolveMetrotvnews(String location) throws MediaOfflineException {
        return "http://edge.metrotvnews.com:1935/live-edge/smil:metro.smil/playlist.m3u8";
    }

    private String resolveCNBCIndonesia(String location) throws MediaOfflineException {
        return "https://live.cnbcindonesia.com/livecnbc/smil:cnbctv.smil/playlist.m3u8";
    }

    private String resolveTVBrasilPlay(String location) throws MediaOfflineException {
        JsonElement root = JsonParser.parseString(fetchUrl("https://play.ebc.com.br/v2/streaming"));

        if (!root.isJsonArray()) {
            return "";
        }

        for (JsonElement item : root.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }

            JsonObject tv = item.getAsJsonObject();
            String streamUrl = getJsonString(tv, "url_streaming");

            if (streamUrl != null && !streamUrl.trim().isEmpty()) {
                return streamUrl;
            }
        }

        return "";
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
        if (location.contains("nbcnews.com/watch#noticias-telemundo-ahora")) {
            return new String[0];
        }

        if (location.contains("globalnews.ca") || location.contains("ctvnews.ca") || location.contains("cp24.com")) {
            return extractLinksFromText(resolveContent(location));
        }

        if (location.contains("rainews.it") || location.contains("i24news.tv") || location.contains("kan.org.il") || location.contains("knesset.tv") || location.contains("mako.co.il") || location.contains("newslive.com") || location.contains("livenewsnow.com") || location.contains("tvpass.org/live/") || location.contains("thetvapp.to/tv/") || location.contains("usnewson.com/watch/") || location.contains("nbcnews.com/watch") || location.contains("streamfare.info/oan-news") || (location.contains("streamfare.") && location.contains("-live-stream") && !location.contains("news-12-new-york-live-stream"))) {
            return new String[]{resolveContent(location)};
        }

        return super.getLinks(location);
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        if (location != null && location.toLowerCase().contains(".m3u8")) {
            String content = getContent(location);
            List<Media> directMedias = new GenericVideoResolver(getWrappedContext())
                    .findMedia(location, location, content, this::buildMediaLink);

            if (directMedias != null && !directMedias.isEmpty()) {
                return directMedias;
            }
        }

        if (location != null && location.contains("abc.com/watch-live/")) {
            String mediaUrl = resolveABCWatchLive(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "ABC News Live",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1280, 720, 0),
                                "",
                                true
                        )
                );
            }
        }

        if (location != null && location.contains("i24news.tv")) {
            String mediaUrl = resolveI24News(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                String info = location.toLowerCase().endsWith("/he") ? "I24 News HE"
                        : location.toLowerCase().endsWith("/en") ? "I24 News EN"
                        : "I24 News";

                return java.util.Collections.singletonList(
                        new Video(
                                info,
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.LOW, 480, 270, 635800),
                                "",
                                true
                        )
                );
            }
        }

        if ("https://www.nbcnews.com/watch".equals(location) || "https://nbcnews.com/watch".equals(location)) {
            String mediaUrl = resolveNBCLowestVariantUrl(location, resolveNBCNewsWatch(location));

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC News",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#telemundo-texas")) {
            String mediaUrl = resolveNBCTelemundoTexasLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Telemundo Texas",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#telemundo-noreste")) {
            String mediaUrl = resolveNBCTelemundoNoresteLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Telemundo Noreste",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#telemundo-florida")) {
            String mediaUrl = resolveNBCTelemundoFloridaLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Telemundo Florida",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#telemundo-california")) {
            String mediaUrl = resolveNBCTelemundoCaliforniaLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Telemundo California",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#san-diego")) {
            String mediaUrl = resolveNBCSanDiegoLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC San Diego",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#philadelphia")) {
            String mediaUrl = resolveNBCPhiladelphiaLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Philadelphia",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#dateline")) {
            String mediaUrl = resolveNBCDatelineLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Dateline",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#dallas-fort-worth")) {
            String mediaUrl = resolveNBCDallasFortWorthLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Dallas Fort Worth",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#connecticut")) {
            String mediaUrl = resolveNBCConnecticutLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Connecticut",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#boston")) {
            String mediaUrl = resolveNBCBostonLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Boston",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#bay-area")) {
            String mediaUrl = resolveNBCBayAreaLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Bay Area",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#los-angeles")) {
            String mediaUrl = resolveNBCLosAngelesLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Los Angeles",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#new-york")) {
            String mediaUrl = resolveNBCNewYorkLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC New York",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("nbcnews.com/watch#washington")) {
            String mediaUrl = resolveNBCWashingtonLowestVariantUrl(location);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                return java.util.Collections.singletonList(
                        new Video(
                                "NBC Washington",
                                mediaUrl,
                                new Video.VideoQuality(Quality.Type.HIGH, 1920, 1080, 5253600),
                                "",
                                true
                        )
                );
            }
        }

        if (location.contains("globalnews.ca") || location.contains("ctvnews.ca") || location.contains("cp24.com")) {
            String parentLink;
            String content;

            if (location.contains("globalnews.ca")) {
                parentLink = resolveGlobalNewsMasterUrl(location);
                content = parentLink == null || parentLink.trim().isEmpty() ? "" : fetchUrl(parentLink);
            } else if (location.contains("ctvnews.ca")) {
                parentLink = resolveBellMedia9c9ManifestUrl(location, "ctvnews_web");
                content = parentLink == null || parentLink.trim().isEmpty() ? "" : fetchUrl(parentLink);
            } else {
                parentLink = resolveBellMedia9c9ManifestUrl(location, "cp24_web");
                content = parentLink == null || parentLink.trim().isEmpty() ? "" : fetchUrl(parentLink);
            }

            if (content == null || content.trim().isEmpty()) {
                throw new MediaOfflineException(String.format("media offline or connection issue %s", location));
            }

            List<Media> medias = new GenericVideoResolver(getWrappedContext()).findMedia(location, parentLink, content, this::buildMediaLink);

            if (medias == null || medias.isEmpty()) {
                throw new MediaNotFoundException(String.format("media not found for location %s", location));
            }

            return medias;
        }

        return super.getMedia(location);
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
        } else if (location.contains("tvbrasilplay.com.br/tvs")) {
            return resolveTVBrasilPlay(location);
        } else if (location.contains("jovempan.com.br/ao-vivo")) {
            return resolveJovemPan(location);
        } else if (location.contains("timesnownews.com")) {
            return resolveTimesNowNews(location);
        } else if (location.contains("rtp.pt")) {
            return resolveRtp(location);
        } else if (location.contains("biobiochile.cl")) {
            return resolveBiochile(location);
        } else if (location.contains("13tv.co.il")) {
            return resolve13TV(location);
        } else if (location.contains("globalnews.ca")) {
            return resolveGlobalNews(location);
        } else if (location.contains("abc.com/watch-live/")) {
            return resolveABCWatchLive(location);
        } else if (location.contains("nbcnews.com/watch")) {
            return resolveNBCNewsWatch(location);
        } else if (location.contains("kan.org.il")) {
            return resolveKan(location);
        } else if (location.contains("knesset.tv")) {
            return resolveKnesset(location);
        } else if (location.contains("mako.co.il")) {
            return resolveMako(location);
        } else if (location.contains("ctvnews.ca")) {
            return resolveBellMedia9c9(location, "ctvnews_web");
        } else if (location.contains("cp24.com")) {
            return resolveBellMedia9c9(location, "cp24_web");
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
