package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;

import java.util.List;

public interface MediaResolver extends Context {

    List<Media> findMedia(String location, String parentLink, String content, MediaLinkBuilder mediaLinkBuilder);
}
