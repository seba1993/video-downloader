package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class FFMPEGSupport extends BaseSupport {

    private static final Pattern LINK_PATTERN = Pattern.compile("(?<link>(https?):\\\\?\\/\\\\?\\/[-a-zA-Z0-9+&@#\\/%?=~_|!:,.\\*;\\[\\]\\\\]*[-a-zA-Z0-9+&@#\\/%=~_|\\[\\]\\\\\\*])");

    public FFMPEGSupport(Context context) {
        super(context);
    }

    protected Map<String, String> getOptions(Media media) {
        return Map.of();
    }

    protected Map<String, String> getHeaders(String location) {
        return Map.of("Referer", LocationRequestUtils.sanitize(location));
    }

    protected String getVideoCopyCodec(Media media) {
        return media instanceof Video ? "-c:v copy" : "";
    }

    protected String getAudioCopyCodec(Media media) {
        return media instanceof Video ? "-c:a copy" : "-vn -f mp3";
    }

    protected String getOutputExtension(Media media) {
        return media instanceof Video ? "mkv" : "mp3";
    }

    protected String getOutputFile(Media media, String baseFileName, String destinationPath) {
        final String extension = getOutputExtension(media);
        final String fileName = String.format("%s.%s", baseFileName, extension);
        final String filePath = buildPath(destinationPath, fileName);

        return filePath;
    }

    protected String generateCommand(String location, Media media, String outputFile) {
        final Map<String, String> commandHeaders = new HashMap<>();
        commandHeaders.putAll(getHeaders(location));
        commandHeaders.putAll(LocationRequestUtils.extractHeaders(location));

        final String options = getOptions(media).entrySet().stream()
                .map(entry -> String.format("-%s %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" "));

        final String headers = commandHeaders.entrySet().stream()
                .map(entry -> String.format("-headers \"%s: %s\"", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" "));

        final String url = LocationRequestUtils.sanitize(media.getUrl());

        final String videoCopyCodec = getVideoCopyCodec(media);
        final String audioCopyCodec = getAudioCopyCodec(media);

        return String.format("ffmpeg -nostdin -xerror -abort_on empty_output %s %s -i \"%s\" %s %s \"%s\"",
                options, headers, url, videoCopyCodec, audioCopyCodec, outputFile);
    }

    protected abstract String resolveContent(String location) throws MediaOfflineException;

    protected boolean isValidLink(String link) {
        return link.toUpperCase().contains(".M3U8");
    }

    @Override
    protected String[] getLinks(String location) throws MediaOfflineException {
        if (isValidLink(location)) {
            return new String[]{location};
        }

        final String content = resolveContent(location);

        ArrayList<String> links = new ArrayList<>();

        Matcher matcher = LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            String link = matcher.group("link");

            if (link.contains("\\/")) {
                link = link.replaceAll("\\\\/", "/");
            }

            if (!isValidLink(link)) continue;

            if (!links.contains(link)) {
                links.add(link);
            }
        }

        return links.toArray(new String[0]);
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        if (!mediaLink.contains("http")) return null;

        if (mediaLink.contains("\\/")) {
            mediaLink = mediaLink.replaceAll("\\\\/", "/");
        }

        return super.buildMediaLink(location, parentLink, mediaLink);
    }

    @Override
    public Task generateTask(String location, Media media, Map<String, Object> params) {
        final String baseFileName = params.get("base_file_name").toString();
        final String destinationPath = params.get("destination_path").toString();

        final String outputFile = getOutputFile(media, baseFileName, destinationPath);

        final String command = generateCommand(location, media, outputFile);

        return new FFMPEGTask(getWrappedContext(), media, command, outputFile, destinationPath);
    }
}
