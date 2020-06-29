package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseMediaResolver extends ContextWrapper implements MediaResolver {

    public BaseMediaResolver(Context context) {
        super(context);
    }

    protected abstract Media parseMediaDescriptor(String location, String parentLink, MediaDescriptor mediaDescriptor, MediaLinkBuilder mediaLinkBuilder);

    protected abstract List<MediaDescriptor> getMediaDescriptors(String location, String parentLink, String content);

    @Override
    public List<Media> findMedia(String location, String parentLink, String content, MediaLinkBuilder mediaLinkBuilder) {
        final List<MediaDescriptor> mediaDescriptors = getMediaDescriptors(location, parentLink, content);

        if (mediaDescriptors.isEmpty()) return Collections.emptyList();

        ArrayList<Media> medias = new ArrayList<>();

        for (MediaDescriptor mediaDescriptor : mediaDescriptors) {
            Media media = parseMediaDescriptor(location, parentLink, mediaDescriptor, mediaLinkBuilder);

            if (media != null) {
                medias.add(media);
            }
        }

        return medias;
    }
}
