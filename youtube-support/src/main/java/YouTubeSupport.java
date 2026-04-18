import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.util.CryptoUtils;
import com.github.luischavez.videodownloader.util.PlatformUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class YouTubeSupport extends FFMPEGSupport {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("\\\"videoId\\\":\\\"(?<id>.[^\\\"]+)");
    private static final Pattern ITAG_PATTERN = Pattern.compile("/itag/(?<itag>\\d+)/");

    private static final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=%s";

    public YouTubeSupport(Context context) {
        super(context);
    }

    private String resolveExecutable(String workingDirectory) {
        final String extension = PlatformUtils.isWindowsHost() ? ".exe" : "";
        final String ytDlpPath = buildPath(workingDirectory, "youtube", "yt-dlp" + extension);

        if (new java.io.File(ytDlpPath).exists()) return "yt-dlp" + extension;

        return "yt-dlp";
    }

    private String resolveLocation(String location) {
        String lower = location.toLowerCase();

        if (!lower.contains("youtube.com")) {
            return location;
        }

        if (lower.contains("watch?v=") || lower.endsWith("/live")) {
            return location;
        }

        if (!lower.contains("/streams")) {
            return location;
        }

        try {
            final String workingDirectory = System.getProperty("user.dir");
            final String executable = resolveExecutable(workingDirectory);
            final File youtubeDirectory = new File(buildPath(workingDirectory, "youtube"));
            final File executableFile = new File(youtubeDirectory, executable);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    executableFile.getPath(),
                    "--ignore-config",
                    "--flat-playlist",
                    "--print", "id",
                    "--playlist-end", "1",
                    location
            );
            processBuilder.directory(youtubeDirectory);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("WARNING:") || line.startsWith("ERROR:") || line.startsWith("[")) {
                        continue;
                    }

                    if (line.matches("[A-Za-z0-9_-]{6,}")) {
                        return String.format(YOUTUBE_LINK, line);
                    }
                }
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return location;
    }

    private String resolveFormat(Media media) {
        Matcher matcher = ITAG_PATTERN.matcher(media.getUrl());
        if (matcher.find()) return matcher.group("itag");

        if (media.getQuality() instanceof Video.VideoQuality) {
            return String.format("bestvideo[height<=%d]+bestaudio/best[height<=%d]",
                    Video.VideoQuality.class.cast(media.getQuality()).getHeight(),
                    Video.VideoQuality.class.cast(media.getQuality()).getHeight());
        }

        return "best";
    }

    @Override
    protected Pattern[] getLocationPatterns() {
        return patterns(Pattern.compile("^https?://.*youtube\\.com.*$"));
    }

    @Override
    protected MediaResolver[] getMediaResolvers() {
        return resolvers(new M3U8VideoResolver(getWrappedContext()));
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        location = resolveLocation(location);
        String[] links = super.getLinks(location);

        return Arrays.asList(links).stream()
                .map(link -> {
                    link = CryptoUtils.decodeUrl(link);

                    if (link.endsWith("\\")) {
                        link = link.substring(0, link.length() - 1);
                    }

                    return link;
                })
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    protected String resolveContent(String location) throws MediaOfflineException {
        location = resolveLocation(location);
        String content = getContent(location);

        if (location.toUpperCase().contains("WATCH")) {
            return content;
        } else {
            Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(content);
            if (matcher.find()) {
                String videoId = matcher.group("id");
                String streamLink = String.format(YOUTUBE_LINK, videoId);

                content = getContent(streamLink);

                return content;
            }
        }

        return "";
    }

    @Override
    public Task generateTask(String location, Media media, Map<String, Object> params) {
        location = resolveLocation(location);
        final String baseFileName = params.get("base_file_name").toString();
        final String destinationPath = params.get("destination_path").toString();

        final String workingDirectory = System.getProperty("user.dir");
        final String executable = resolveExecutable(workingDirectory);
        final String format = resolveFormat(media);
        final String outputTemplate = buildPath(destinationPath, baseFileName) + ".%(ext)s";

        final List<String> arguments = List.of(
                "--ignore-config",
                "--cookies-from-browser", "firefox",
                "--no-part",
                "--hls-use-mpegts",
                "--newline",
                "--merge-output-format", "mkv",
                "-f", format,
                "-o", outputTemplate,
                location
        );

        return new YouTubeDLTask(getWrappedContext(), media, executable, arguments, destinationPath);
    }
}
