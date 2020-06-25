package com.github.luischavez.videodownloader.support;

import java.util.List;

public interface MediaResolver<M extends Media> {

    boolean hasMedia(String content);
    List<M> resolve(String content);
}
