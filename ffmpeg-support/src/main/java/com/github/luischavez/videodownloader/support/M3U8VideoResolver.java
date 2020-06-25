package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.system.Injected;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8VideoResolver extends BaseMediaResolver<Video> {

    private static final Pattern[] M3U8_OPTION_PATTERNS = {
            Pattern.compile("(?<info>#EXT.*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*BANDWIDTH=(?<bandwidth>\\d+).*)\\n(?<m3u8>.+)", Pattern.MULTILINE),
            Pattern.compile("(?<info>#EXT.*BANDWIDTH=(?<bandwidth>\\d+).*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*)\\n(?<m3u8>.+)", Pattern.MULTILINE),
            Pattern.compile("(?<info>#EXT.*BANDWIDTH=(?<bandwidth>\\d+).*)\\n(?<m3u8>.+)", Pattern.MULTILINE),
    };

    private static final Pattern M3U8_CODECS_PATTERN = Pattern.compile("#EXT.*CODECS=\\\"(?<codecs>.[^\\\"]+).*");

    @Injected
    public M3U8VideoResolver(Context context) {
        super(context);
    }

    @Override
    public boolean hasMedia(String content) {
        for (Pattern pattern : M3U8_OPTION_PATTERNS) {
            if (pattern.matcher(content).find()) return true;
        }

        return false;
    }

    @Override
    public List<Video> resolve(String content) {
        if (content.toUpperCase().contains("#EXTINF")) return Collections.emptyList();

        Pattern selectedPattern = null;

        for (Pattern pattern : M3U8_OPTION_PATTERNS) {
            if (pattern.matcher(content).find()) {
                selectedPattern = pattern;
                break;
            }
        }

        if (selectedPattern != null) {
            ArrayList<Video> videos = new ArrayList<>();

            Matcher matcher = selectedPattern.matcher(content);

            while (matcher.find()) {
                String info = matcher.group("info");

                int width = 0;
                int height = 0;

                try {
                    width = Integer.valueOf(matcher.group("width"));
                    height = Integer.valueOf(matcher.group("height"));
                } catch (Exception ex) {
                    // NO RESOLUTION DATA FOUND.
                }

                int bandwidth = Integer.valueOf(matcher.group("bandwidth"));
                String url = matcher.group("m3u8");

                Quality.Type type;

                if (bandwidth < 900000) {
                    type = Quality.Type.LOW;
                } else if (bandwidth > 3000000) {
                    type = Quality.Type.HIGH;
                } else {
                    type = Quality.Type.MEDIUM;
                }

                Video.VideoQuality quality = new Video.VideoQuality(type, width, height, bandwidth);

                String codecs = "";
                Matcher matcherCodecs = M3U8_CODECS_PATTERN.matcher(info);
                if (matcherCodecs.find()) codecs = matcherCodecs.group("codecs");

                Video video = new Video(info, url, quality, codecs);
                videos.add(video);
            }

            ArrayList<Video> chunked = new ArrayList<>();
            for (Video video : videos) {
                if (video.getInfo().toUpperCase().contains("CHUNKED")) {
                    chunked.add(video);
                }
            }

            if (chunked.size() != videos.size()) {
                videos.removeAll(chunked);
            }

            return videos;
        }

        return Collections.emptyList();
    }
}
