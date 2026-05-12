package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8VideoResolver extends BaseMediaResolver {

    private static final Pattern[] M3U8_OPTION_PATTERNS = {
            Pattern.compile("(?<descriptor>(?<info>#EXT.*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*BANDWIDTH=(?<bandwidth>\\d+).*)\\r?\\n(?<m3u8>.+))", Pattern.MULTILINE),
            Pattern.compile("(?<descriptor>(?<info>#EXT.*BANDWIDTH=(?<bandwidth>\\d+).*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*)\\r?\\n(?<m3u8>.+))", Pattern.MULTILINE),
            Pattern.compile("(?<descriptor>(?<info>#EXT.*BANDWIDTH=(?<bandwidth>\\d+).*)\\r?\\n(?<m3u8>.+))", Pattern.MULTILINE),
            Pattern.compile("(?<descriptor>(?<info>#EXT.*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*BANDWIDTH=(?<bandwidth>\\d+.*URI=\\\"(?<m3u8>.*[^\\\"])).*)\\r?\\n(?<m3u82>.+))", Pattern.MULTILINE),
    };

    private static final Pattern M3U8_CODECS_PATTERN = Pattern.compile("#EXT.*CODECS=\\\"(?<codecs>.[^\\\"]+).*");

    public M3U8VideoResolver(Context context) {
        super(context);
    }

    @Override
    protected Media parseMediaDescriptor(String location, String parentLink, MediaDescriptor mediaDescriptor, MediaLinkBuilder mediaLinkBuilder) {
        final String info = mediaDescriptor.get("info");
        final String m3u8 = mediaDescriptor.get("m3u8");
        final int bandwidth = Integer.valueOf(mediaDescriptor.get("bandwidth"));
        final int width = Integer.valueOf(mediaDescriptor.get("width"));
        final int height = Integer.valueOf(mediaDescriptor.get("height"));
        final boolean main = !info.toUpperCase().contains("CHUNKED");

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

        String mediaLink = m3u8;
        if (mediaLinkBuilder != null) mediaLink = mediaLinkBuilder.buildMediaLink(location, parentLink, mediaLink);

        if (mediaLink == null) return null;

        return new Video(info, mediaLink, quality, codecs, main);
    }

    @Override
    protected List<MediaDescriptor> getMediaDescriptors(String location, String parentLink, String content) {
        if (content.toUpperCase().contains("#EXTINF")) {
            MediaDescriptor descriptor = new MediaDescriptor();
            descriptor.put("info", "");
            descriptor.put("m3u8", parentLink);
            descriptor.put("bandwidth", "0");
            descriptor.put("width", "0");
            descriptor.put("height", "0");

            return Collections.singletonList(descriptor);
        }

        ArrayList<MediaDescriptor> descriptors = new ArrayList<>();

        for (Pattern pattern : M3U8_OPTION_PATTERNS) {
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                final String info = matcher.group("info");
                final String m3u8 = matcher.group("m3u8");
                final String bandwidth = matcher.group("bandwidth");

                String width = "0";
                String height = "0";

                try {
                    width = matcher.group("width");
                    height = matcher.group("height");
                } catch (Exception ex) {
                    // NO RESOLUTION DATA FOUND.
                }

                MediaDescriptor descriptor = new MediaDescriptor();
                descriptor.put("info", info);
                descriptor.put("m3u8", m3u8);
                descriptor.put("bandwidth", bandwidth);
                descriptor.put("width", width);
                descriptor.put("height", height);

                descriptors.add(descriptor);
            }

            if (!descriptors.isEmpty()) break;
        }

        return descriptors;
    }
}
