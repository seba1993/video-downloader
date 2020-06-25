package com.github.luischavez.videodownloader.support;

public interface Media extends Comparable<Media> {

    String getInfo();

    String getUrl();

    Quality getQuality();

    String getCodec();
}
