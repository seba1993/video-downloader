package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Support<M extends Media> extends Context {

    Pattern DEFAULT_M3U8_LINK_PATTERN = Pattern.compile("(?<link>https?(.[^\\\"]*)m3u8)");

    Pattern[] getPatterns();

    String getContent(String location) throws MediaOfflineException;

    MediaResolver<M> getMediaResolver();

    Task generateTask(Media media, Map<String, Object> params);

    default List<M> getMedia(String location) throws MediaOfflineException, MediaNotFoundException {
        String content = getContent(location);

        MediaResolver<M> mediaResolver = getMediaResolver();

        if (!mediaResolver.hasMedia(content)) throw new MediaNotFoundException("media not found " + location);

        List<M> medias = mediaResolver.resolve(content);

        if (medias == null || medias.isEmpty()) throw new MediaNotFoundException("can't resolve media " + location);

        return medias;
    }

    default Pattern[] patterns(Pattern... patterns) {
        return patterns;
    }

    default List<String> findLinks(String content, Pattern pattern) {
        ArrayList<String> links = new ArrayList<>();

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String link = matcher.group("link");

            if (!links.contains(link)) {
                links.add(link);
            }
        }

        return links;
    }
}
