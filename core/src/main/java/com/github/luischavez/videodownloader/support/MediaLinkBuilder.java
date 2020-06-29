package com.github.luischavez.videodownloader.support;

@FunctionalInterface
public interface MediaLinkBuilder {

    String buildMediaLink(String location, String parentLink, String mediaLink);
}
